import Model.ChannelPool;
import Model.SwipeEvent;
import Model.UserLikesDislikes;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

public class Consumer1 {

  private static ChannelPool channelPool;
  private static final int THREAD_POOL_SIZE = 200;
  private static final int NUM_CHANNELS = 30;
  private final static String QUEUE_NAME = "swipeQueue1";
  private static Gson gson = new Gson();
  private static UserLikesDislikes userLikesDislikes = new UserLikesDislikes();
  private static ExecutorService executorService;
  private static String tableName = "swipeData";

  public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
    // Create channel pool
    try {
      channelPool = new ChannelPool(NUM_CHANNELS, QUEUE_NAME);
      executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }

    AwsCredentialsProvider credentialsProvider = SystemPropertyCredentialsProvider.create();
    System.setProperty("aws.accessKeyId", "ASIAYHSTMVQC7HARPZ72");
    System.setProperty("aws.secretAccessKey", "3ztobe8hZe/TU9xB7RxxqYy+n8uBdh9f1Xmyj6wg");
    System.setProperty("aws.sessionToken",
        "FwoGZXIvYXdzEL3//////////wEaDIZtIEvIZ8Habs/wZiLMARSPqjSYDFBmuswuuy8uVdDDCCrtZPXFe2sYRaHQyw+pXIrU1wqoYlB33vRWE2elMMH1pcdfXFnlPubHKHhCjBM4lZVZTdYL0EAVIqXdGrSYzL1Bf4BlmniGkdjVYlantyukMw+3mZcQSP5nWprgYtgd2SIjBWEcSgdR0jLFN/Y0hwal3DsfMpk5Le/GZHRMB/aFoRkn8Bq9MsYcQOXv5UmWJ/ZSf9Py4S6PPiQUuf4iO5XvKpBZZTo1+TAkd8QDgNZdvu2TSN7gnLN76ij7tJmhBjItIHxbaXnp9nRR623+XRQxQOqYHJwfQbUemLqthwm2aDaUo20+YiufOOnAlJ2Q");

    DynamoDbClient client = DynamoDbClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(Region.US_WEST_2)
        .build();

    // Create multiple consumers to process messages in parallel
    List<Consumer> consumers = new ArrayList<>();
    for (int i = 0; i < NUM_CHANNELS; i++) {
      Channel curChannel = channelPool.borrowChannel();
      Consumer consumer = new DefaultConsumer(curChannel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
          String message = new String(body, "UTF-8");
          try {
            executorService.execute(() -> processMessage(message, client));
            curChannel.basicAck(envelope.getDeliveryTag(), false);
          } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            boolean requeueMessage = true;
            curChannel.basicNack(envelope.getDeliveryTag(), false, requeueMessage);
          }
        }
      };
      curChannel.basicConsume(QUEUE_NAME, false, consumer);
      consumers.add(consumer);
    }

    System.out.println("Waiting for messages...");
  }

  private static void processMessage(String message, DynamoDbClient client) {
    SwipeEvent swipe = gson.fromJson(message, SwipeEvent.class);
    String swiper = swipe.getSwiper();
    HashMap<String, AttributeValue> keyToGet = new HashMap<>();
    keyToGet.put("swiper", AttributeValue.builder().s(swiper).build());
    GetItemRequest request = GetItemRequest.builder()
        .tableName(tableName)
        .key(keyToGet)
        .build();

    GetItemResponse response = client.getItem(request);
    if (response.hasItem()) {
      updateTableItem(swipe, response, client);
    } else {
      putItemInTable(swipe, client);
    }
    //    String leftOrRight = swipe.getSwipe();
//    String userId = swipe.getSwiper();
//    if (leftOrRight.equals("left")) {
//      userLikesDislikes.addUserDislike(userId);
//    } else if (leftOrRight.equals("right")) {
//      userLikesDislikes.addUserLike(userId);
//    } else {
//      throw new IllegalArgumentException("Invalid swipe: " + leftOrRight);
//    }
//    System.out.println(swipe);
  }

  private static void putItemInTable(SwipeEvent swipe, DynamoDbClient client){
    String swiper = swipe.getSwiper();
    String swipee = swipe.getSwipee();
    String leftOrRight = swipe.getSwipe();
    int likes = 0;
    int dislikes = 0;
    List<String> potentialMatches = new ArrayList<>();
    if (Objects.equals(leftOrRight, "left")){
      likes += 1;
      potentialMatches.add(swipee);
    } else {
      dislikes += 1;
    }
    Map<String, AttributeValue> item = new HashMap<>();
    item.put("swiper", AttributeValue.builder().s(swiper).build());
    item.put("likes", AttributeValue.builder().n(String.valueOf(likes)).build());
    item.put("dislikes", AttributeValue.builder().n(String.valueOf(dislikes)).build());
    Gson gson = new Gson();
    try {
      String personJson = gson.toJson(potentialMatches);
      item.put("potentialMatches", AttributeValue.builder().s(personJson).build());
    } catch (JsonIOException e) {
      e.printStackTrace();
    }

    PutItemRequest request = PutItemRequest.builder()
        .tableName(tableName)
        .item(item)
        .build();
    try {
      client.putItem(request);
      System.out.println(tableName +" was successfully updated");

    } catch (ResourceNotFoundException e) {
      System.err.format("Error: The Amazon DynamoDB table \"%s\" can't be found.\n", tableName);
      System.err.println("Be sure that it exists and that you've typed its name correctly!");
    } catch (DynamoDbException e) {
      System.err.println(e.getMessage());
    }

  }
  private static void updateTableItem(SwipeEvent swipe, GetItemResponse response, DynamoDbClient client){
    String swiper = swipe.getSwiper();
    String swipee = swipe.getSwipee();
    String leftOrRight = swipe.getSwipe();

    Map<String, AttributeValue> item = response.item();
    Map<String, AttributeValue> newItem = new HashMap<>();

    int likes = Integer.parseInt(item.get("likes").n());
    int dislikes = Integer.parseInt(item.get("dislikes").n());

    ArrayList potentialMatches = new Gson().fromJson(item.get("potentialMatches").s(), ArrayList.class);
    if (Objects.equals(leftOrRight, "left")){
      likes += 1;
      potentialMatches.add(swipee);
    } else {
      dislikes += 1;
    }
    newItem.put("swiper", AttributeValue.builder().s(swiper).build());
    newItem.put("likes", AttributeValue.builder().n(String.valueOf(likes)).build());
    newItem.put("dislikes", AttributeValue.builder().n(String.valueOf(dislikes)).build());
    Gson gson = new Gson();
    try {
      String potentialMatchesJson = gson.toJson(potentialMatches);
      newItem.put("potentialMatches", AttributeValue.builder().s(potentialMatchesJson).build());
    } catch (JsonIOException e) {
      e.printStackTrace();
    }
    PutItemRequest request = PutItemRequest.builder()
        .tableName(tableName)
        .item(newItem)
        .build();

    try {
      client.putItem(request);
      System.out.println(tableName +" was successfully updated");
    } catch (ResourceNotFoundException e) {
      System.err.format("Error: The Amazon DynamoDB table \"%s\" can't be found.\n", tableName);
      System.err.println("Be sure that it exists and that you've typed its name correctly!");
    } catch (DynamoDbException e) {
      System.err.println(e.getMessage());
    }
  }

}



