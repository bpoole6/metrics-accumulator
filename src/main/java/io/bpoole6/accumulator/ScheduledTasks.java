package io.bpoole6.accumulator;

import io.bpoole6.accumulator.service.RegistryRepository;
import io.bpoole6.accumulator.service.MetricGroupConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
public class ScheduledTasks {

    private final TaskScheduler executor;
    private final RegistryRepository metrics;
    private final MetricGroupConfiguration metricGroupConfiguration;
    private final List<Task> tasks = new ArrayList<>();

    public ScheduledTasks(@Qualifier("taskScheduler") TaskScheduler taskExecutor,
                          RegistryRepository metrics,
                          MetricGroupConfiguration metricGroupConfiguration) {
        this.executor = taskExecutor;
        this.metrics = metrics;
        this.metricGroupConfiguration = metricGroupConfiguration;
        reset();
    }

    public void scheduling(String name, final Runnable task, String cronExpression) {
        CronTrigger trigger = new CronTrigger(cronExpression);

        ScheduledFuture<?> f = executor.schedule(task,trigger );
        Task t= new Task(name, trigger, f);
        tasks.add(t);
    }

    public void reset(){
        for (int i = 0; i < tasks.size(); i++) {
            try{
                tasks.get(i).getFuture().cancel(true);
            }catch (Exception e){
                log.error("Failed to cancel %s".formatted(tasks.get(i).getName()));
            }
        }
        tasks.clear();
        metrics.getRegistryMap().forEach((group, metricManager) -> {
            String restartCronExpression = group.getRestartCronExpression();
            if(restartCronExpression == null) {
                restartCronExpression = metricGroupConfiguration.getGlobal().getRestartCronExpression();
            }
            scheduling(group.getName(), () -> {
                try {
                    metricManager.resetRegistries();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }, restartCronExpression);
        });
    }
}