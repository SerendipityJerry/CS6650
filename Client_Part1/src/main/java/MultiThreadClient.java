import Model.SwipeEvent;
import Model.SwipeEventConsumer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import Model.SwipeEventProducer;

public class MultiThreadClient {

  private static final int totalReq = 500000;
  private static final int numOfThread = 80;
  private static final int firstNumWork = 6250;

  public static void main(String[] args) throws InterruptedException {
    String urlIP = "http://35.162.40.20:8080/Server_war/";

    BlockingQueue<SwipeEvent> swipeEvents = new LinkedBlockingQueue<>();
    AtomicInteger successReq = new AtomicInteger(0);
    AtomicInteger failReq = new AtomicInteger(0);

    System.out.println("*********************************************************");
    System.out.println("Processing Begins");
    System.out.println("*********************************************************");

    CountDownLatch latch = new CountDownLatch(numOfThread);
    long start = System.currentTimeMillis();
    SwipeEventProducer eventsGenerator = new SwipeEventProducer(swipeEvents,totalReq);
    Thread producerThread = new Thread(eventsGenerator);
    producerThread.start();

    for (int i = 0; i < numOfThread; i++) {
      SwipeEventConsumer consumer = new SwipeEventConsumer(urlIP, firstNumWork, successReq, failReq,
          swipeEvents, latch);
      Thread thread = new Thread(consumer);
      thread.start();
    }
    latch.await();

    long end = System.currentTimeMillis();
    long wallTime = end - start;

    System.out.println("*********************************************************");
    System.out.println("Processing Ends");
    System.out.println("*********************************************************");
    System.out.println("Number of successful requests :" + successReq.get());
    System.out.println("Number of failed requests :" + failReq.get());
    System.out.println("Total wall time: " + wallTime);
    System.out.println( "Throughput: " + (int)((successReq.get() + failReq.get()) / (double)(wallTime / 1000)) + " requests/second");
  }
}
