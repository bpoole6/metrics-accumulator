package io.bpoole6.accumulator.service;

import io.bpoole6.accumulator.service.metricgroup.Global;
import io.bpoole6.accumulator.service.metricgroup.Group;
import io.bpoole6.accumulator.service.metricgroup.Root;
import java.util.function.Function;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Reason for using a configuration file over an Enum.
 * <p>
 * 1.) Because we are using an apikey to control access to updating the metrics we need a seperation
 * between dev/prod. An enum class wouldn't be able to easily differentiate between the two profiles
 * like you can do with configurations files
 */
@Component
@Data
@Slf4j
public class MetricsAccumulatorConfiguration {
    public static final String CONFIGURATION_OPTS = "config-file";
    private String hostAddress;
    private Global global;
    private Map<String, Group> metricGroups;
    private Map<String, Group> metricGroupByApiKey;
    private String configurationFile;
    private String fileContent;
    public MetricsAccumulatorConfiguration(ApplicationArguments arguments)
            throws IOException {
        if(!arguments.containsOption(CONFIGURATION_OPTS)){
            log.error("--%s option is not set".formatted(CONFIGURATION_OPTS));
            throw new RuntimeException();
        }
        configurationFile = Paths.get(arguments.getOptionValues(CONFIGURATION_OPTS).get(0)).toAbsolutePath().toString();
        resetConfiguration();
    }

    public void resetConfiguration() throws IOException {
        File file = ResourceUtils.getFile(configurationFile);
        Yaml yaml = new Yaml();
        String content = Files.readString(file.toPath());
        Root root = yaml.loadAs(content, Root.class);
        validateConfigs(root);
        this.metricGroups = root.getMetricGroups().values().stream().collect(Collectors.toMap(i->i.getName(),
            Function.identity()));
        this.hostAddress = root.getGlobal().getHostAddress();
        this.metricGroupByApiKey = metricGroups.values().stream().collect(Collectors.toMap(Group::getApiKey, g -> g));
        this.global = root.getGlobal();
        this.fileContent = content;
    }

    public List<String> getNames() {
        return metricGroups.values().stream().filter(Group::isDisplayMetrics).map(
                Group::getName).toList();
    }

    private void validateConfigs(Root root){
        StringJoiner sj = new StringJoiner(System.lineSeparator());
        if(Objects.isNull(root)){
            sj.add("Configuration file parsed but no configs present");
        }
        if(Objects.isNull(root.getGlobal())){
            sj.add("global attribute not set");
        }else {
            if(Objects.isNull(root.getGlobal().getRestartCronExpression())){
                sj.add("global.restartCronExpression not set");
            }
            if(Objects.isNull(root.getGlobal().getHostAddress())){
                sj.add("global.hostAddress not set");
            }
        }

        if(Objects.isNull(root.getMetricGroups())){
            sj.add("metricGroups is not set");
        } else if (root.getMetricGroups().isEmpty()){
            sj.add("metricGroups is empty");
        }else{
            root.getMetricGroups().forEach((k,v)->{
                if(Objects.isNull(v.getName())){
                    sj.add("name is unset for metricGroup %s".formatted(k));
                }else if (!v.getName().matches("^[-a-zA-Z0-9_]+")){
                    sj.add("name doesn't match regex ^[-a-zA-Z0-9_]+");
                }
                if(Objects.isNull(v.getApiKey())){
                    sj.add("apiKey is unset for metricGroup %s".formatted(k));
                }
                if(Objects.isNull(v.getMaxTimeSeries())){
                    sj.add("maxTimeSeries is unset for metricGroup %s".formatted(k));
                }
            });
        }
        String errors = sj.toString();
        if(!errors.isBlank()){
            log.error(errors);
            throw new RuntimeException("Failed metric group configurations");
        }

    }
}
