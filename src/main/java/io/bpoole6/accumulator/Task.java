package io.bpoole6.accumulator;

import lombok.Data;
import org.springframework.scheduling.support.CronTrigger;

import java.util.concurrent.ScheduledFuture;

@Data
public class Task {
    private final String name;
    private final CronTrigger cronTrigger;
    private final ScheduledFuture<?> future;

}
