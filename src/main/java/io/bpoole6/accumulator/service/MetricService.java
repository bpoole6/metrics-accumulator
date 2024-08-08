package io.bpoole6.accumulator.service;

import io.bpoole6.accumulator.ScheduledTasks;
import io.bpoole6.accumulator.service.metricgroup.Group;
import io.bpoole6.accumulator.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import prometheus.types.Metric;
import prometheus.types.MetricFamily;
import prometheus.types.MetricType;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@Slf4j
public class MetricService {

  private final MetricsAccumulatorConfiguration metricsAccumulatorConfiguration;
  private final ScheduledTasks scheduledTasks;
  private RegistryRepository registryRepository;
  private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

  public MetricService(RegistryRepository registryRepository, MetricsAccumulatorConfiguration metricsAccumulatorConfiguration, ScheduledTasks scheduledTasks) {
    this.registryRepository = registryRepository;
    this.metricsAccumulatorConfiguration = metricsAccumulatorConfiguration;
    this.scheduledTasks = scheduledTasks;
  }

  public void modifyMetrics(String metrics, Group metricGroup) throws IOException, InterruptedException {
    ReentrantReadWriteLock.ReadLock readlock = reentrantReadWriteLock.readLock();
    if (readlock.tryLock(20, TimeUnit.SECONDS)) {
      try {
        synchronized (metricGroup) {
          List<MetricFamily> metricFamilies = Utils.readMetrics(metrics);
          for (MetricFamily metricFamily : metricFamilies) {
            for (Metric metric : metricFamily.getMetrics()) {
              MetricKey metricKey = new MetricKey(metricGroup, metricFamily.getName(), metricFamily.getHelp(), metric.getLabels());
              if (metricFamily.getType() == MetricType.COUNTER) {
                this.registryRepository.incrementCounter(metricKey, (prometheus.types.Counter) metric);
              } else if (metricFamily.getType() == MetricType.GAUGE) {
                this.registryRepository.setGauge(metricKey, (prometheus.types.Gauge) metric);
              }
            }
          }
        }
      }catch (Exception e) {
        log.error("Failed to modify metrics",e);
      }finally {
        readlock.unlock();
      }
    }
  }

  public Optional<String> getMetricSnapshot(Group metricGroup) {

    MetricManager mm = registryRepository.getRegistry(metricGroup);
    if (mm != null) {
      return Optional.of(mm.getPrometheusRegistry().scrape());
    }
    return Optional.empty();
  }

  public boolean resetMetricGroup(Group group) {
    try {
      MetricManager metricManager = this.registryRepository.getRegistry(group);
      metricManager.resetRegistries();
    }catch (Exception e){
      return false;
    }
    return true;
  }
  public boolean resetConfigs() throws InterruptedException {
    ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();
    if(writeLock.tryLock(20,TimeUnit.SECONDS)) {
      try {
        metricsAccumulatorConfiguration.resetConfiguration();
        registryRepository.reset();
        scheduledTasks.reset();
        return true;
      }catch (Throwable e){
        log.error("Failed to reset. Please reset configurations");
        return false;
      }
      finally {
        writeLock.unlock();
      }
    }else{
      return false;
    }
  }
}
