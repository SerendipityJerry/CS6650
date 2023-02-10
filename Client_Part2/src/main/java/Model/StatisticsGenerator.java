package Model;

import Model.OutputRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StatisticsGenerator {
  private List<OutputRecord> records;

  public StatisticsGenerator(List<OutputRecord> records) {
    this.records = records;
    Collections.sort(this.records, (a, b) -> Long.compare(a.getLatency(), b.getLatency()));
  }

  public double getMeanResponse() {
    long sum = 0;
    for (OutputRecord record : this.records) {
      sum += record.getLatency();
    }

    return (double) sum / this.records.size();
  }

  public double getMedianResponse() {
    int size = this.records.size();
    int midIndex = size / 2;
    if (size % 2 == 0) {
      return (records.get(midIndex - 1).getLatency() + records.get(midIndex).getLatency()) / 2.0;
    } else {
      return records.get(midIndex).getLatency();
    }
  }

  public double get99Percentile() {
    int index = (int) (0.99 * this.records.size());
    return records.get(index).getLatency();
  }

  public double getMinResponse() {
    return records.get(0).getLatency();
  }

  public double getMaxResponse() {
    return records.get(records.size() - 1).getLatency();
  }

  public Integer[] get_plot() {
    List<Long> startTimes = new ArrayList<>(records.size());
    for (OutputRecord record : records) {
      startTimes.add(record.getStartTime());
    }
    Collections.sort(startTimes);

    long minStartTime = startTimes.get(0);
    int counterSize = (int) ((startTimes.get(startTimes.size() - 1) - minStartTime) / 1000) + 1;
    Integer[] counter = new Integer[counterSize];
    Arrays.fill(counter, 0);

    for (Long startTime : startTimes) {
      int index = (int) ((startTime - minStartTime) / 1000);
      counter[index]++;
    }

    return counter;
  }
}



