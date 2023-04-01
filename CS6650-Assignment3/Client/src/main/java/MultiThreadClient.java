import Model.ChartGenerator;
import Model.GetEventGenerator;
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
  private static final int numOfThread = 100;
  private static final int threadNumWork = 5000;

  public static void main(String[] args) throws InterruptedException, IOException {
//    String urlIP = "http://localhost:8080/Server_war_exploded";
    String urlIP = "http://load-balancer-6650-944868980.us-west-2.elb.amazonaws.com:8080/Server_war/";

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
      Thread postThread = new Thread(consumer);
      postThread.start();
    }

    List<OutputRecord> getRecords = Collections.synchronizedList(new ArrayList<>());
    GetEventGenerator getEventGenerator = new GetEventGenerator(urlIP, totalReq, successReq,failReq,getRecords);
    Thread getThread = new Thread(getEventGenerator);
    getThread.start();

    latch.await();

    long end = System.currentTimeMillis();
    long wallTime = end - start;

    try {
      writeCVSFile(allRecords, "/Users/wangzhaolei/Documents/Assignment1/Client_Part2/src/main/java/Output/Records.csv");
    } catch (IOException e) {
      e.printStackTrace();
    }

    StatisticsGenerator postStatistics = new StatisticsGenerator(allRecords);
    double meanResTime = postStatistics.getMeanResponse();
    double medianResTime = postStatistics.getMedianResponse();
    double percentile99ResTime = postStatistics.get99Percentile();
    double minResTime = postStatistics.getMinResponse();
    double maxResTime = postStatistics.getMaxResponse();

    System.out.println("*********************************************************");
    System.out.println("Processing Ends");
    System.out.println("*********************************************************");
    System.out.println("Total wall time: " + wallTime);
    System.out.println("Statistics of post requests:");
    System.out.println("Number of successful post requests :" + successReq.get());
    System.out.println("Number of failed post requests :" + failReq.get());
    System.out.println("Mean response time (in milliseconds):" + meanResTime);
    System.out.println("Median response time (in milliseconds):" + medianResTime);
    System.out.println( "Throughput:" + (int)((successReq.get() + failReq.get()) /
        (double)(wallTime / 1000) )+ " requests/second");
    System.out.println("P99 (99th percentile) response time (in milliseconds):" + percentile99ResTime);
    System.out.println("Min response time (in milliseconds):" + minResTime);
    System.out.println("Max response time (in milliseconds):" + maxResTime);

    StatisticsGenerator getStatistics = new StatisticsGenerator(allRecords);
    double getMinResTime = getStatistics.getMinResponse();
    double getMeanResTime = getStatistics.getMeanResponse();
    double getMaxResTime = getStatistics.getMaxResponse();
    System.out.println("Statistics of get requests:");
    System.out.println("Number of successful post requests :" + getEventGenerator.getSuccessCount());
    System.out.println("Number of failed post requests :" + getEventGenerator.getFailCount());
    System.out.println("Min response time (in milliseconds):" + getMinResTime);
    System.out.println("Mean response time (in milliseconds):" + getMeanResTime);
    System.out.println("Max response time (in milliseconds):" + getMaxResTime);

    Integer[] plot_data = postStatistics.get_plot();
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

