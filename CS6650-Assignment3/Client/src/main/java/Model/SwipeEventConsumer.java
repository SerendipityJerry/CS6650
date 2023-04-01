package Model;

import static java.net.HttpURLConnection.HTTP_CREATED;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import io.swagger.client.model.SwipeDetails;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class SwipeEventConsumer implements Runnable{
  private String urlIP;
  private int totalReq;
  private AtomicInteger successReq;
  private AtomicInteger failReq;
  private BlockingQueue<SwipeEvent> swipeEvents;
  private CountDownLatch latch;
  private static final int MAX_RETRY = 5;
  private List<OutputRecord> records;

  public SwipeEventConsumer(String urlIP, int totalReq, AtomicInteger successReq, AtomicInteger failReq,
      BlockingQueue<SwipeEvent> swipeEvents, CountDownLatch latch, List<OutputRecord> records) {
    this.urlIP = urlIP;
    this.totalReq = totalReq;
    this.successReq = successReq;
    this.failReq = failReq;
    this.swipeEvents = swipeEvents;
    this.latch = latch;
    this.records = records;
  }

  @Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    SwipeApi swipeApi = new SwipeApi(apiClient);
    swipeApi.getApiClient().setBasePath(this.urlIP);
    int successCount = 0;
    int failCount = 0;

    for (int i = 0; i < this.totalReq; i++) {
      SwipeEvent curEvent = this.swipeEvents.poll();
      if (postWithEvent(swipeApi, curEvent)) {
        successCount++;
      } else {
        failCount++;
      }
    }

    successReq.getAndAdd(successCount);
    failReq.getAndAdd(failCount);
    latch.countDown();
  }

  public boolean postWithEvent(SwipeApi swipeApi, SwipeEvent curEvent) {
    int retry = 0;

    while (retry < MAX_RETRY) {
      try {
        long start = System.currentTimeMillis();
        SwipeDetails swipeRes = new SwipeDetails();
        swipeRes.setSwiper(curEvent.getSwiper());
        swipeRes.setSwipee(curEvent.getSwipee());
        swipeRes.setComment(curEvent.getComment());
        ApiResponse<Void> res = swipeApi.swipeWithHttpInfo(swipeRes, curEvent.getSwipe());
        if (res.getStatusCode() == HTTP_CREATED) {
          long end = System.currentTimeMillis();
          System.out.println(end - start);
          this.records.add(new OutputRecord(start, "POST",end - start, res.getStatusCode()));
          return true;
        }
      } catch (ApiException e) {
        retry++;
        e.printStackTrace();
      }
    }

    return false;
  }
}
