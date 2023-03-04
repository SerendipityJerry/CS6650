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
  private final static String EXCHANGE_NAME = "swipeExchange";
  private final static String QUEUE_NAME_1 = "swipeQueue1";
  private final static String QUEUE_NAME_2 = "swipeQueue2";

  public ChannelPool(int num_of_channels) throws IOException, TimeoutException {
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

    for (int i = 0; i < num_of_channels; i++) {
      try {
        System.out.println(i);
        Channel channel = this.connection.createChannel();
//        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT, true, false, false, null
//        );
        channel.queueDeclare(QUEUE_NAME_1, true, false, false, null);
        channel.queueDeclare(QUEUE_NAME_2, true, false, false, null);
//        channel.queueBind(QUEUE_NAME_1, EXCHANGE_NAME, "");
//        channel.queueBind(QUEUE_NAME_2, EXCHANGE_NAME, "");
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
