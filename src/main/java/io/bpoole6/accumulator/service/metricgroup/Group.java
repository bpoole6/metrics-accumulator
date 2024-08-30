package io.bpoole6.accumulator.service.metricgroup;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Group {
    private boolean displayMetrics;
    private String name;
    private Integer maxTimeSeries;
    private String apiKey;
    private String restartCronExpression;
    private final Map<String, String> serviceDiscoveryLabels = new HashMap<>();

    public void setServiceDiscoveryLabels(Map<String,String> serviceDiscoveryLabels){
        this.serviceDiscoveryLabels.clear();
        this.serviceDiscoveryLabels.putAll(serviceDiscoveryLabels);
    }
}
