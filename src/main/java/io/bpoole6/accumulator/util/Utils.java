package io.bpoole6.accumulator.util;

import io.bpoole6.accumulator.service.metricgroup.Group;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import prometheus.text.TextPrometheusMetricDataParser;
import prometheus.types.MetricFamily;
import prometheus.types.MetricType;

public class Utils {
  public static List<MetricFamily> readMetrics(String metrics) throws IOException {

    TextPrometheusMetricDataParser i = new TextPrometheusMetricDataParser(
        new ByteArrayInputStream(metrics.getBytes()));
    List<MetricFamily> metricFamilies = new ArrayList<>();
    MetricFamily family;
    while ((family = i.parse()) != null) {
      if((family.getType() == MetricType.COUNTER || family.getType() == MetricType.GAUGE) && !family.getName().endsWith("_info")) {
        metricFamilies.add(family);
      }
    }
    return metricFamilies;
  }
  public static boolean exceedsMaxTimeSeriesLimit(Group group, PrometheusMeterRegistry registry){
    synchronized (registry){
      return registry.getPrometheusRegistry().scrape().size() >= group.getMaxTimeSeries();
    }
  }
}
