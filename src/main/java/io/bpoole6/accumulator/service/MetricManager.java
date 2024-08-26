package io.bpoole6.accumulator.service;

import io.bpoole6.accumulator.util.RunnableThrowable;
import io.bpoole6.accumulator.util.Utils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import lombok.Getter;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class MetricManager {

    @Getter
    private final PrometheusMeterRegistry prometheusRegistry;
    private static final Logger log = LoggerFactory.getLogger(RegistryRepository.class);
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private final Map<MetricKey, Counter> counters = new ConcurrentHashMap<>();
    private final Map<MetricKey, MetricValue> gauges = new ConcurrentHashMap<>();

    private final String metricGroup;

    public MetricManager(String metricGroup) {
        prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        this.metricGroup = metricGroup;
    }

    public void incrementCounter(MetricKey key, prometheus.types.Counter incrementer) {

        RunnableThrowable runnable = () -> {
            StringJoiner sj = new StringJoiner(",");
            key.getTags().forEach((k, v) -> {
                sj.add(k);
                sj.add(v);
            });
            String[] labels = sj.toString().split(",");
            PrometheusMeterRegistry pr = prometheusRegistry;
            Counter counter = this.counters.computeIfAbsent(key, (k) -> {
                if (Utils.exceedsMaxTimeSeriesLimit(key.getMetricGroup(), pr)) {
                    return null;
                }
                Counter.Builder builder = Counter.builder(
                        key.getMetricName()).description(key.getHelp());
                if (!key.getTags().isEmpty()) {
                    builder.tags(labels);
                }
                return builder.register(pr);
            });

            counter.increment(incrementer.getValue());
        };
        runReadLockProcess(runnable);
    }

    public void setGauge(MetricKey key, prometheus.types.Gauge metric) {
        RunnableThrowable runnable = () -> {
            StringJoiner sj = new StringJoiner(",");
            Map<String, String> tags = RegistryRepository.stripMetaData(key.getTags());
            Long timestampOpt = latestTimestamp(key.getTags()).orElse(0L);

            tags.forEach((k, v) -> {
                sj.add(k);
                sj.add(v);
            });
            String[] labels = sj.toString().split(",");
            PrometheusMeterRegistry pr = prometheusRegistry;
            MetricValue mv = new MetricValue(metric.getValue(), timestampOpt);

            MetricValue foundGague = gauges.computeIfAbsent(key, k -> {
                if (Utils.exceedsMaxTimeSeriesLimit(key.getMetricGroup(), pr)) {
                    return null;
                }
                Gauge.Builder builder = Gauge.builder(k.getMetricName(), mv, MetricValue::getValue)
                        .description(key.getHelp());
                if (!tags.isEmpty()) {
                    builder.tags(labels);
                }
                builder.register(pr);
                return mv;
            });

            if (foundGague != null && foundGague.getTimestamp() <= timestampOpt) {
                foundGague.setValue(mv.getValue());
                foundGague.setTimestamp(mv.getTimestamp());
            }
        };
        runReadLockProcess(runnable);
    }

    public Optional<Long> latestTimestamp(Map<String, String> tags) {
        if (tags.containsKey(RegistryRepository.META_DATA_LATEST) && NumberUtils.isCreatable(tags.get(
                RegistryRepository.META_DATA_LATEST))) {
            return Optional.of(Long.parseLong(tags.get(RegistryRepository.META_DATA_LATEST)));
        }
        return Optional.empty();
    }


    public void resetRegistries() throws InterruptedException {
        RunnableThrowable runnable = () -> {
            this.counters.clear();
            this.gauges.clear();
            this.prometheusRegistry.clear();
        };
        runWriteLockProcess(runnable);
    }

    public void runWriteLockProcess(RunnableThrowable runnableThrowable) throws InterruptedException {
        WriteLock lock = reentrantReadWriteLock.writeLock();
        if (lock.tryLock(10, TimeUnit.SECONDS)) {
            try {
                runnableThrowable.run();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            } finally {
                lock.unlock();
            }
        } else {
            throw new RuntimeException("Write lock fail for %s".formatted(this.metricGroup));
        }
    }

    public void runReadLockProcess(RunnableThrowable runnableThrowable) {
        ReadLock lock = reentrantReadWriteLock.readLock();
        try {
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                try {
                    runnableThrowable.run();
                } catch (Throwable e) {
                } finally {
                    lock.unlock();
                }
            } else {
                throw new RuntimeException("Read lock fail for %s".formatted(this.metricGroup));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
