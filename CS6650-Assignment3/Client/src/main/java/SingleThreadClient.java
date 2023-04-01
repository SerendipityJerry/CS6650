import Model.ChartGenerator;
import Model.OutputRecord;
import Model.StatisticsGenerator;
import Model.SwipeEvent;
import Model.SwipeEventConsumer;
import Model.SwipeEventProducer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleThreadClient {
  private static final int TOTAL_REQ = 1000;

  public static void main(String[] args) throws InterruptedException, IOException {
    String urlIP = "http://localhost:8080/Server_war_exploded/";
//    String urlIP = "http://35.85.53.126:8080/Server_war/";


    // LinkedBlockingQueue是一个线程安全的阻塞队列，实现了先进先出等特性，是作为生产者消费者的首选，可以指定容量，也可以不指定，不指定的话默认最大是Integer.MAX_VALUE
    // 其中主要用到put和take方法，put方法将一个对象放到队列尾部，在队列满的时候会阻塞直到有队列成员被消费，
    // take方法从head取一个对象，在队列为空的时候会阻塞，直到有队列成员被放进来。
    // offer方法添加元素，如果队列已满，直接返回false
    BlockingQueue<SwipeEvent> swipeEvents = new LinkedBlockingQueue<>();
    AtomicInteger successReq = new AtomicInteger(0);
    AtomicInteger failReq = new AtomicInteger(0);
    List<OutputRecord> allRecords = Collections.synchronizedList(new ArrayList<>());

    System.out.println("*********************************************************");
    System.out.println("Processing Begins");
    System.out.println("*********************************************************");

    // 在java中是最常用的获取系统时间的方法,它返回的是1970年1月1日0点到现在经过的毫秒数。
    long start = System.currentTimeMillis();
    SwipeEventProducer eventsGenerator = new  SwipeEventProducer(swipeEvents,TOTAL_REQ);
    Thread producerThread = new Thread(eventsGenerator);
    producerThread.start();

    CountDownLatch latch = new CountDownLatch(1);
    SwipeEventConsumer consumer = new SwipeEventConsumer(urlIP, TOTAL_REQ, successReq, failReq, swipeEvents, latch, allRecords);
    Thread consumerThread = new Thread(consumer);
    consumerThread.start();
    latch.await();

    long end = System.currentTimeMillis();
    long wallTime = end - start;

    try {
      writeCVSFile(allRecords, "/Users/wangzhaolei/Documents/Assignment1/Client_Part2/src/main/java/Output/Records.csv");
    } catch (IOException e) {
      e.printStackTrace();
    }

    StatisticsGenerator statistics = new StatisticsGenerator(allRecords);
    double meanResTime = statistics.getMeanResponse();
    double medianResTime = statistics.getMedianResponse();
    double percentile99ResTime = statistics.get99Percentile();
    double minResTime = statistics.getMinResponse();
    double maxResTime = statistics.getMaxResponse();


    System.out.println("*********************************************************");
    System.out.println("Processing Ends");
    System.out.println("*********************************************************");
    System.out.println("Number of successful requests :" + successReq.get());
    System.out.println("Number of failed requests :" + failReq.get());
    System.out.println("Total wall time: " + wallTime);
    System.out.println("Mean response time (in milliseconds):" + meanResTime);
    System.out.println("Median response time (in milliseconds):" + medianResTime);
    System.out.println( "Throughput:" + (int)((successReq.get() + failReq.get()) /
        (double)(wallTime / 1000) )+ " requests/second");
    System.out.println("P99 (99th percentile) response time (in milliseconds):" + percentile99ResTime);
    System.out.println("Min response time (in milliseconds):" + minResTime);
    System.out.println("Max response time (in milliseconds):" + maxResTime);

    Integer[] plot_data = statistics.get_plot();
    for (int i = 0; i < plot_data.length; i++) {
      System.out.println("Second" + i + " 's throughput: " + plot_data[i]);
    }

    ChartGenerator chartGenerator = new ChartGenerator(plot_data, "/Users/wangzhaolei/Documents/Assignment1/Client_Part2/src/main/java/Output/PlotPerformance.xlsx");
  }

  public static void writeCVSFile(List<OutputRecord> records, String filepath)
      throws IOException {
    File csv = new File(filepath);
    if(csv.exists()) {
      csv.delete();
    }
    FileWriter writer = new FileWriter(csv);
    writer.write("StartTime,RequestType,Latency,ResponseCode\n");
    for (OutputRecord record: records) {
      writer.write(record.toString() + "\n");
    }
    writer.close();
  }
}

