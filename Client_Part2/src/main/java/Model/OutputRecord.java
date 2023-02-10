package Model;

public class OutputRecord implements Comparable<OutputRecord> {
  private long startTime;
  private String requestType;
  private long latency;
  private int responseCode;

  public OutputRecord(long startTime, long latency, int responseCode) {
    this.startTime = startTime;
    this.requestType = "POST";
    this.latency = latency;
    this.responseCode = responseCode;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getLatency() {
    return latency;
  }

  public void setLatency(long latency) {
    this.latency = latency;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  @Override
  public int compareTo(OutputRecord o) {
    if (this.getLatency() - o.getLatency() > 0) {
      return 1;
    } else if (this.getLatency() - o.getLatency() == 0) {
      return 0;
    }
    return -1;
  }

  public String toString() {
    return this.getStartTime() + ",POST," + this.getLatency() + "," + this.getResponseCode();
  }
}