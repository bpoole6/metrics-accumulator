package io.bpoole6.accumulator.service;

import lombok.Data;

@Data
public class MetricValue {
  private Double value;
  private long timestamp;
  public MetricValue(Double value, long timestamp) {
    this.value = value;
    this.timestamp = timestamp;
  }
}
