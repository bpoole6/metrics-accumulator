package io.bpoole6.accumulator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestUtils {
	public static String DEFAULT_METRIC_NAME = "test-Metrics";
	public static Path createMetricsFile() throws IOException {
		Path path = Files.createTempFile("config",".yml");
		Files.writeString(path,metricsFile(DEFAULT_METRIC_NAME));
		return path;
	}
  public static String metricsFile(String name){
    return """
						global:
						  restartCronExpression: "0 0 0 ? * *"
						  hostAddress: localhost:8080
						      
						metricGroups:
						  %s:
						    displayMetrics: true
						    name: %s
						    maxTimeSeries: 100
						    apiKey: 0d98f65f-074b-4d56-b834-576e15a3bfa5
						    restartCronExpression: "0 0 0 ? * *"
						    serviceDiscoveryLabels:
						      env: qa
						      version: v45    
						""".formatted(name,name);
  }

}
