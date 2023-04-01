package Model;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.*;

public class ChannelPool {

  private Connection connection;
  private BlockingQueue<Channel> pool;
  private final static String QUEUE_NAME = "swipeQueue";
  public ChannelPool(int num_of_channels) throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
    factory.setHost("52.33.64.123");
    factory.setUsername("admin");
    factory.setPassword("admin");

    try {
      this.connection = factory.newConnection();
    } catch (IOException | TimeoutException e) {
      System.err.println("Something Went Wrong in Connection");
      e.printStackTrace();
    }

    this.pool = new LinkedBlockingQueue<>();

    for (int i = 0; i < num_of_channels; i++) {
      try {
        System.out.println(i);
        Channel channel = this.connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
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
