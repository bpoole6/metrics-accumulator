package io.bpoole6.accumulator.service;

import io.bpoole6.accumulator.service.metricgroup.Group;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RegistryRepository {

  public static final String META_DATA_PREFIX = "_metrics_accumulator_";
  public static final String META_DATA_LATEST = META_DATA_PREFIX + "latest";

  @Getter
  private final Map<Group, MetricManager> registryMap = new HashMap<>();
  private final MetricGroupConfiguration metricGroupConfiguration;

  public RegistryRepository(MetricGroupConfiguration metricGroupConfiguration) {
   this.metricGroupConfiguration = metricGroupConfiguration;
    reset();
  }

  public void reset(){
    registryMap.clear();
    for (Group metricGroup : metricGroupConfiguration.getMetricGroups().values()) {
      registryMap.put(metricGroup, new MetricManager(metricGroup.getName()));
    }
  }
  public MetricManager getRegistry(Group metricGroup) {
    return this.registryMap.get(metricGroup);
  }
  public void incrementCounter(MetricKey metricKey, prometheus.types.Counter metric) {
    this.registryMap.get(metricKey.getMetricGroup()).incrementCounter(metricKey, metric);
  }
  public void setGauge(MetricKey metricKey, prometheus.types.Gauge metric) {
    this.registryMap.get(metricKey.getMetricGroup()).setGauge(metricKey, metric);
  }



  public static Map<String, String> stripMetaData(Map<String, String> labels) {
    Map<String, String> map = new HashMap<>();
    labels.forEach((k, v) -> {
      if (!k.startsWith(META_DATA_PREFIX)) {
        map.put(k, v);
      }
    });
    return map;
  }
}
