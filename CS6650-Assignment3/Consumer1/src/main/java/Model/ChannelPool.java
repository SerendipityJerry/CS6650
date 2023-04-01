package Model;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ChannelPool {

  private Connection connection;
  private BlockingQueue<Channel> pool;

  public ChannelPool(int numOfChannels, String queueName) throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
    factory.setHost("52.38.83.195");
    factory.setUsername("admin");
    factory.setPassword("admin");

    try {
      this.connection = factory.newConnection();
    } catch (IOException | TimeoutException e) {
      System.err.println("Something Went Wrong in Connection");
      e.printStackTrace();
    }

    this.pool = new LinkedBlockingQueue<>();

    for (int i = 0; i < numOfChannels; i++) {
      try {
        Channel channel = this.connection.createChannel();
        channel.queueDeclare(queueName, true, false, false, null);
        this.pool.add(channel);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public Channel borrowChannel() throws InterruptedException {
    return this.pool.take();
  }

  public void returnChannel(Channel channel) {
    this.pool.offer(channel);
  }

  public void closeChannelPool() throws InterruptedException, IOException, TimeoutException {
    while (!this.pool.isEmpty()) {
      Channel channel = this.pool.take();
      channel.close();
      System.out.println(this.pool.size());
    }
    this.connection.close();
  }

}
