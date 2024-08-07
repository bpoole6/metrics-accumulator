package io.bpoole6.accumulator.service.metricgroup;

import lombok.Data;

@Data
public class Group {
    private boolean displayMetrics;
    private String name;
    private Integer maxTimeSeries;
    private String apiKey;
    private String restartCronExpression;
}
