package Model;

import static java.net.HttpURLConnection.HTTP_CREATED;

import io.swagger.client.ApiClient;

import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.StatsApi;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import io.swagger.client.api.MatchesApi;
import io.swagger.client.model.MatchStats;
import io.swagger.client.model.Matches;

public class GetEventGenerator implements Runnable{
  private String urlIP;
  private int totalReq;
  private AtomicInteger successReq;
  private AtomicInteger failReq;
  private static final int MAX_RETRY = 5;
  private List<OutputRecord> records;
  private int successCount = 0;
  private int failCount = 0;
  private static final int MIN_SWIPER_ID = 1;
  private static final int MAX_SWIPER_ID = 5000;
  private static final int REQUESTS_PER_SECOND = 5;

  public GetEventGenerator(String urlIP, int totalReq, AtomicInteger successReq,
      AtomicInteger failReq, List<OutputRecord> records) {
    this.urlIP = urlIP;
    this.totalReq = totalReq;
    this.successReq = successReq;
    this.failReq = failReq;
    this.records = records;
  }

  @Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    MatchesApi matchesApi = new MatchesApi(apiClient);
    matchesApi.getApiClient().setBasePath(urlIP);
    StatsApi statsApi = new StatsApi(apiClient);

    while (!(successReq.get() + failReq.get() == totalReq)) {
      for (int i = 0; i < REQUESTS_PER_SECOND; i++){
        String swiperID = Integer.toString(
            ThreadLocalRandom.current().nextInt(MIN_SWIPER_ID, MAX_SWIPER_ID + 1));
        Integer curGetEvent = ThreadLocalRandom.current().nextInt(0, 2);
        boolean getStatus;
        if (curGetEvent == 0) {
          getStatus = getMatch(matchesApi, swiperID);
        } else {
          getStatus = getStats(statsApi, swiperID);
        }
        if (getStatus) {
          this.successCount += 1;
        } else {
          this.failCount += 1;
        }
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private boolean getStats(StatsApi statsApi, String swiperID) {
    int retry = 0;

    while (retry < MAX_RETRY) {
      try {
        long start = System.currentTimeMillis();
        ApiResponse<MatchStats> res = statsApi.matchStatsWithHttpInfo(swiperID);
        if (res.getStatusCode() == HTTP_CREATED) {
          long end = System.currentTimeMillis();
          System.out.println(end - start);
          this.records.add(new OutputRecord(start, "GET",end - start, res.getStatusCode()));
          return true;
        }
      } catch (ApiException e) {
        retry++;
        e.printStackTrace();
      }
    }

    return false;
  }

  private boolean getMatch(MatchesApi matchesApi, String swiperID) {
    int retry = 0;

    while (retry < MAX_RETRY) {
      try {
        long start = System.currentTimeMillis();
        ApiResponse<Matches> res = matchesApi.matchesWithHttpInfo(swiperID);
        if (res.getStatusCode() == HTTP_CREATED) {
          long end = System.currentTimeMillis();
          System.out.println(end - start);
          this.records.add(new OutputRecord(start, "GET",end - start, res.getStatusCode()));
          return true;
        }
      } catch (ApiException e) {
        retry++;
        e.printStackTrace();
      }
    }

    return false;
  }

  public int getSuccessCount() {
    return successCount;
  }

  public int getFailCount() {
    return failCount;
  }
}

