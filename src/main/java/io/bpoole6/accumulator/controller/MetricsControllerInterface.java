package io.bpoole6.accumulator.controller;

import io.bpoole6.accumulator.controller.response.ConfigurationResponse;
import io.bpoole6.accumulator.controller.response.ServiceDiscovery;
import io.bpoole6.accumulator.service.metricgroup.Group;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;
import java.util.Optional;



interface MetricsControllerInterface {


  @GetMapping("/")
  public ModelAndView serverStatus(ModelMap map);
  @Operation(
          summary = "Reloaded the loaded configuration. All Metrics will be erased",
          description = "Reloads configuration that were passed in via --config-file. The File source will be reread from storage. All Metrics will be erased")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "succeeded to reload configurations"),
          @ApiResponse(responseCode = "417", description = "failed to reload configurations")
  })

  @PutMapping(value = "reload-configuration")
  ResponseEntity<Object> reloadConfig() throws InterruptedException;

  @Operation(
          summary = "Erases the metric group metrics out of memory.",
          description = "Erases the metric group metrics out of memory.",
          parameters = {
                  @Parameter(name = "metricGroup",  required = true,  in = ParameterIn.PATH,description = "The name of your metric group",example = "default")
          }
  )
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "succeeded to erase metrics for metric group"),
          @ApiResponse(responseCode = "417", description = "failed to erase metrics for metric group")
  })
  @PutMapping(value = "reset-metric-group/{metricGroup}")
  ResponseEntity<Object> resetMetricGroup(@PathVariable("metricGroup") String metricGroup) throws InterruptedException;


  @Operation(
          summary = "Updates metric group metrics.",
          description = "Updates metric group metrics.",
          parameters = {
                  @Parameter(name = "X-API-KEY",  required = true,  in = ParameterIn.HEADER, example = "0d98f65f-074b-4d56-b834-576e15a3bfa5"),
                  @Parameter(name = "metricGroup",  required = true,  in = ParameterIn.PATH,description = "The name of your metric group",example = "default")
          })
  @ApiResponses(value = {
          @ApiResponse(responseCode = "201", description = "Succeeded to update the configurations"),
          @ApiResponse(responseCode = "404", description = "Metric group doesn't exist"),
          @ApiResponse(responseCode = "401", description = "You authenticated but apiKey doesn't correspond to metric Group supplied"),
  })
  @PostMapping(value = "update/{metricGroup}", consumes = {"text/plain"})
  ResponseEntity<String> updateMetrics(@PathVariable("metricGroup") String metricGroup, @RequestBody String metrics)
          throws IOException, InterruptedException;

  @Operation(
          summary = "Returns the metrics for a metrics group.",
          description = "Returns the metrics for a metrics group.",
  parameters = {
          @Parameter(name = "metricGroup",  required = true,  in = ParameterIn.PATH, example = "default")
          })
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Succeeded to get metric group's metrics"),
          @ApiResponse(responseCode = "404", description = "Metric group doesn't exist"),
  })
  @GetMapping(value = "metrics/{metricGroup}", produces = {"text/plain"})
  ResponseEntity<String> metrics(@PathVariable("metricGroup") String metricName);

  @Operation(
          summary = "A service discovery mechanism for prometheus",
          description = "A service discovery mechanism for prometheus\nPlease see documentation https://prometheus.io/docs/prometheus/latest/http_sd/")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Returns service discovery successfully"),
  })
  @GetMapping(value = "service-discovery", produces = "application/json")
  ResponseEntity<List<ServiceDiscovery>> serviceDiscovery();

  @Operation(
          summary = "The current configurations loaded",
          description = "Copy string literal text to the clipboard")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200"),
  })
  @GetMapping(value = "current-configurations")
  ResponseEntity<ConfigurationResponse> currentConfigurations();

  Optional<Group> getMetricGroupFromSecurityContext();
}
