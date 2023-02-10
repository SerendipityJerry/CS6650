import Model.SwipeEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import Model.SwipeEventConsumer;
import Model.SwipeEventProducer;

public class SingleThreadClient {
  private static final int TOTAL_REQ = 10000;

  public static void main(String[] args) throws InterruptedException {
    String urlIP = "http://35.162.40.20:8080/Server_war/";


    // LinkedBlockingQueue是一个线程安全的阻塞队列，实现了先进先出等特性，是作为生产者消费者的首选，可以指定容量，也可以不指定，不指定的话默认最大是Integer.MAX_VALUE
    // 其中主要用到put和take方法，put方法将一个对象放到队列尾部，在队列满的时候会阻塞直到有队列成员被消费，
    // take方法从head取一个对象，在队列为空的时候会阻塞，直到有队列成员被放进来。
    // offer方法添加元素，如果队列已满，直接返回false
    BlockingQueue<SwipeEvent> swipeEvents = new LinkedBlockingQueue<>();
    AtomicInteger successReq = new AtomicInteger(0);
    AtomicInteger failReq = new AtomicInteger(0);

    System.out.println("*********************************************************");
    System.out.println("Processing Begins");
    System.out.println("*********************************************************");

    // 在java中是最常用的获取系统时间的方法,它返回的是1970年1月1日0点到现在经过的毫秒数。
    long start = System.currentTimeMillis();
    SwipeEventProducer eventsGenerator = new SwipeEventProducer(swipeEvents,TOTAL_REQ);
    Thread producerThread = new Thread(eventsGenerator);
    producerThread.start();

    CountDownLatch latch = new CountDownLatch(1);
    SwipeEventConsumer consumer = new SwipeEventConsumer(urlIP, TOTAL_REQ, successReq, failReq, swipeEvents, latch);
    Thread consumerThread = new Thread(consumer);
    consumerThread.start();
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
