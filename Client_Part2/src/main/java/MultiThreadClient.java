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

public class MultiThreadClient {

  private static final int totalReq = 500000;
  private static AtomicInteger successReq;
  private static AtomicInteger failReq;
  private static BlockingQueue<SwipeEvent> swipeEvents;
  private static final int numOfThread = 80;
  private static final int threadNumWork = 6250;

  public static void main(String[] args) throws InterruptedException, IOException {
    String urlIP = "http://35.162.40.20:8080/Server_war/";

    swipeEvents = new LinkedBlockingQueue<>();
    successReq = new AtomicInteger(0);
    failReq = new AtomicInteger(0);

//   CopyOnWriteArrayList和Collections.synchronizedList是实现线程安全的列表的两种方式。
//   其中CopyOnWriteArrayList的写操作性能较差，而多线程的读操作性能较好。
//   而Collections.synchronizedList的写操作性能比CopyOnWriteArrayList在多线程操作的情况下要好很多，
//   而读操作因为是采用了synchronized关键字的方式，其读操作性能并不如CopyOnWriteArrayList。

//    List<OutputRecord> allRecords = new CopyOnWriteArrayList<>();
    List<OutputRecord> allRecords = Collections.synchronizedList(new ArrayList<>());

    System.out.println("*********************************************************");
    System.out.println("Processing Begins");
    System.out.println("*********************************************************");

    CountDownLatch latch = new CountDownLatch(numOfThread);
    long start = System.currentTimeMillis();
    SwipeEventProducer eventsGenerator = new  SwipeEventProducer(swipeEvents,totalReq);
    Thread producerThread = new Thread(eventsGenerator);
    producerThread.start();

    for (int i = 0; i < numOfThread; i++) {
      SwipeEventConsumer consumer = new SwipeEventConsumer(urlIP, threadNumWork, successReq, failReq, swipeEvents, latch, allRecords);
      Thread thread = new Thread(consumer);
      thread.start();
    }
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
//    for (int i = 0; i < plot_data.length; i++) {
//      System.out.println("Second" + i + " 's throughput: " + plot_data[i]);
//    }

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

