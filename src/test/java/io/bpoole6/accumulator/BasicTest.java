package io.bpoole6.accumulator;

import io.bpoole6.accumulator.service.MetricsAccumulatorConfiguration;
import org.mockito.Mockito;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@ContextConfiguration(classes = BasicTest.TestApplicationArguments.class)
public class BasicTest {

  @TestConfiguration
  public static class TestApplicationArguments{
    @Bean("springApplicationArguments")
    public ApplicationArguments getApplicationArguments() throws IOException {
      ApplicationArguments arguments = Mockito.mock(ApplicationArguments.class);
      Path path = TestUtils.createMetricsFile();
      List<String> configFiles = new ArrayList<>();
      configFiles.add(path.toAbsolutePath().toString());
      Mockito.when(arguments.containsOption(MetricsAccumulatorConfiguration.CONFIGURATION_OPTS)).thenReturn(true);
      Mockito.when(arguments.getOptionValues(MetricsAccumulatorConfiguration.CONFIGURATION_OPTS)).thenReturn(configFiles);

      return  arguments;
    }
  }
}
