package io.bpoole6.accumulator.service;

import io.bpoole6.accumulator.ScheduledTasks;
import io.bpoole6.accumulator.service.metricgroup.Group;
import io.bpoole6.accumulator.util.Utils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import prometheus.types.Metric;
import prometheus.types.MetricFamily;
import prometheus.types.MetricType;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@Slf4j
public class MetricService {

  @Data
  private static class MetricQueueItem {
    private final Metric metric;
    private final MetricKey metricKey;
    private final MetricType metricType;
  }

  private final int maxBatchedMetrics;
  public final int sleepTimeBtwMetrics;
  private final MetricsAccumulatorConfiguration metricsAccumulatorConfiguration;
  private final ScheduledTasks scheduledTasks;
  private final RegistryRepository registryRepository;
  private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

  private final BlockingDeque<MetricQueueItem> metricQueueItems = new LinkedBlockingDeque<>();

  public MetricService(RegistryRepository registryRepository, MetricsAccumulatorConfiguration metricsAccumulatorConfiguration,
                       ScheduledTasks scheduledTasks, @Value("${maxBatchedMetrics}") int maxBatchedMetrics,
                       @Value("${sleepTimeBtwMetrics}")int sleepTimeBtwMetrics)  {
    this.registryRepository = registryRepository;
    this.metricsAccumulatorConfiguration = metricsAccumulatorConfiguration;
    this.scheduledTasks = scheduledTasks;
    this.maxBatchedMetrics = maxBatchedMetrics;
    this.sleepTimeBtwMetrics = sleepTimeBtwMetrics;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.execute(this::pollQueue);

  }

  public void modifyMetrics(String metrics, Group metricGroup) throws IOException, InterruptedException {

    List<MetricFamily> metricFamilies = Utils.readMetrics(metrics);
    for (MetricFamily metricFamily : metricFamilies) {
      for (Metric metric : metricFamily.getMetrics()) {
        MetricKey metricKey = new MetricKey(metricGroup, metricFamily.getName(), metricFamily.getHelp(), metric.getLabels());
        metricQueueItems.push(new MetricQueueItem(metric, metricKey, metricFamily.getType()));
      }
    }
  }

  private void persistMetrics(Queue<MetricQueueItem> itemsToBeProcessed) throws InterruptedException {
    ReentrantReadWriteLock.ReadLock readlock = reentrantReadWriteLock.readLock();
    if (readlock.tryLock(20, TimeUnit.SECONDS)) {
      try {
        MetricQueueItem item;
        while ((item = itemsToBeProcessed.poll()) != null) {
          if (item.getMetricType() == MetricType.COUNTER) {
            this.registryRepository.incrementCounter(item.getMetricKey(), (prometheus.types.Counter) item.getMetric());
          } else if (item.getMetricType() == MetricType.GAUGE) {
            this.registryRepository.setGauge(item.getMetricKey(), (prometheus.types.Gauge) item.getMetric());
          }
        }
      } catch (Exception e) {
        log.error("Failed to modify metrics", e);
      } finally {
        readlock.unlock();
      }
    }
  }

  public void pollQueue() {
    while (true) {
      try {

        MetricQueueItem item;
        int count = 0;
        Queue<MetricQueueItem> itemsToBeProcessed = new LinkedList<>();
        while ((item = metricQueueItems.poll()) != null && count < maxBatchedMetrics) {
          itemsToBeProcessed.add(item);
          count++;
        }
        persistMetrics(itemsToBeProcessed);

      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
      try {
        Thread.sleep(sleepTimeBtwMetrics);
      } catch (InterruptedException e) {
       log.error(e.getMessage(), e);
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
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  public boolean resetConfigs() throws InterruptedException {
    ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();
    if (writeLock.tryLock(20, TimeUnit.SECONDS)) {
      try {
        metricsAccumulatorConfiguration.resetConfiguration();
        registryRepository.reset();
        scheduledTasks.reset();
        return true;
      } catch (Throwable e) {
        log.error("Failed to reset. Please reset configurations");
        return false;
      } finally {
        writeLock.unlock();
      }
    } else {
      return false;
    }
  }
}
