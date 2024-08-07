package io.bpoole6.accumulator.service;

import io.bpoole6.accumulator.service.metricgroup.Group;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class MetricKey {

    private final Group metricGroup;
    private final String metricName;
    private final String help;
    private final Map<String, String> tags = new HashMap<>();

    public MetricKey(Group metricGroup, String metricName, String help, Map<String, String> tags) {
        this.metricGroup = metricGroup;
        this.metricName = metricName;
        this.help = help;
        setTags(tags);
    }

    private void setTags(Map<String, String> tags) {
        if (this.tags.isEmpty())
            this.tags.putAll(tags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetricKey metricKey = (MetricKey) o;
        return metricGroup == metricKey.metricGroup && Objects.equals(metricName,
                metricKey.metricName) && Objects.equals(RegistryRepository.stripMetaData(tags), RegistryRepository.stripMetaData(metricKey.tags));
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricGroup, metricName, RegistryRepository.stripMetaData(tags));
    }
}
