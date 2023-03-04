import Model.ChannelPool;
import Model.SwipeEvent;
import Model.UserLikesDislikes;
import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Consumer1 {

  private static ChannelPool channelPool;
  private static final int THREAD_POOL_SIZE = 200;
  private static final int NUM_CHANNELS = 30;
  private final static String QUEUE_NAME = "swipeQueue1";
  private static Gson gson = new Gson();
  private static UserLikesDislikes userLikesDislikes = new UserLikesDislikes();
  private static ExecutorService executorService;

  public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
    // Create channel pool
    try {
      channelPool = new ChannelPool(NUM_CHANNELS, QUEUE_NAME);
      executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }

    // Create multiple consumers to process messages in parallel
    List<Consumer> consumers = new ArrayList<>();
    for (int i = 0; i < NUM_CHANNELS; i++) {
      Channel curChannel = channelPool.borrowChannel();
      Consumer consumer = new DefaultConsumer(curChannel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
          String message = new String(body, "UTF-8");
          try {
            executorService.execute(() -> processMessage(message));
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

  private static void processMessage(String message) {
    SwipeEvent swipe = gson.fromJson(message, SwipeEvent.class);
    String leftOrRight = swipe.getSwipe();
    String userId = swipe.getSwiper();
    if (leftOrRight.equals("left")) {
      userLikesDislikes.addUserDislike(userId);
    } else if (leftOrRight.equals("right")) {
      userLikesDislikes.addUserLike(userId);
    } else {
      throw new IllegalArgumentException("Invalid swipe: " + leftOrRight);
    }
    System.out.println(swipe);
  }

}



