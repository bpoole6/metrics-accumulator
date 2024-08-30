package io.bpoole6.accumulator.controller.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ServiceDiscovery {

  private List<String> targets;
  private Map<String,String> labels;
  public ServiceDiscovery(String metricGroupName, String target, Map<String, String> serviceDiscoveryLabels) {
    labels = new HashMap<>(serviceDiscoveryLabels);
    targets = new ArrayList<>();
    targets.add(target);
    labels.put("__meta_metrics_path", metricGroupName);
  }
}
