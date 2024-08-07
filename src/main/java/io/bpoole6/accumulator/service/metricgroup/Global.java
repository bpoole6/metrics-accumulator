package io.bpoole6.accumulator.service.metricgroup;

import lombok.Data;

@Data
public class Global {
    private String restartCronExpression;
    private String hostAddress;
}
