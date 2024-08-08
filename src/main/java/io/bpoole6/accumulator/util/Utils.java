package io.bpoole6.accumulator.util;

import io.bpoole6.accumulator.service.metricgroup.Group;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
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

  public static void main(String[] args) {
    PrometheusRegistry r = new PrometheusRegistry();
    io.prometheus.metrics.core.metrics.Counter c = io.prometheus.metrics.core.metrics.Counter.builder().name("test").labelNames("tag").register(r);
    io.prometheus.metrics.core.metrics.Counter d = io.prometheus.metrics.core.metrics.Counter.builder().name("test").labelNames("b").register(r);
    c.labelValues("a").inc();
    d.labelValues("b").inc();
    System.out.println();
    CompositeMeterRegistry s = new CompositeMeterRegistry();
    Counter.builder("test")
            .tag("tag", "tag").register(s).increment(3);
    Counter.builder("test")
            .tag("bag", "bag").register(s).increment(34);
    System.out.println();

  }
}
