<h1 align="center" style="border-bottom: none">
   <img alt="Metrics Accumulator" src="/documentation/images/logo.svg" width="600"><br>Metrics Accumulator
</h1>

<!-- TOC -->
  * [Description](#description)
  * [Features](#features)
  * [Program Arguments](#program-arguments)
  * [Getting Started](#getting-started)
    * [Docker](#docker)
    * [Locally](#locally)
  * [Supported Types](#supported-types)
    * [Counters](#counters)
    * [Gauges](#gauges)
  * [How to Utilize this service.](#how-to-utilize-this-service)
    * [Configurations](#configurations)
      * [Under Global](#under-global)
      * [Under MetricGroups](#under-metricgroups)
  * [Service Discovery](#service-discovery)
    * [How Does It Work?](#how-does-it-work)
<!-- TOC -->

## Description
The metric accumulator will accumulate additively time-series metrics for ephemeral jobs such as. 

- GCP CloudRun
- GCP Functions
- AWS Lambdas
- Kubernetes Jobs
- Cron Jobs running somewhere
- ETC

This is an alternative to Prometheus Pushgateway for when you need persistent data on "subsequent" metric pushes.

## Features
- Aggregates metrics
- Has TTL for metrics
- Hot reload configurations

## Program Arguments
|Argument| Description                    | Example                                                     | Required |
| --- |--------------------------------|-------------------------------------------------------------|----------|
|--config-file | path to the configuration file | /metrics-accumulator.jar --config-file=/path/to/configs.yml | yes      |

### API
| Api Endpoint                      | Method | Required Headers | Description                                                                                                                          |
|-----------------------------------|--------|------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| /reset-metric-group/{metricGroup} | PUT    | N/A              | Erases the metric group metrics out of memory.                                                                                       |
| /reload-configuration             | PUT    | N/A              | Reloads configuration that were passed in via --config-file. The File source will be reread from storage. All Metrics will be erased |
| /update/{metricGroup}             | POST   | X-API-KEY        | Updates metric group metrics.                                                                                                        |
| /service-discovery             | GET    | N/A              | A service discovery mechanism for prometheus Please see documentation https://prometheus.io/docs/prometheus/latest/http_sd/          |
| /metrics/{metricGroup}            | GET    | N/A              | Returns the metrics for a metrics group.                                                                                             |
| /current-configuations            | GET    | N/A              | Displays the current loaded configurations                                                                                           |
| /swagger-ui/index.html#/            | GET    | N/A              | Swagger Endpoint                                                                                                                     |

## Getting Started

### Docker
Start docker container
```bash
 docker run \ 
   -p 8080:8080 \ 
   bpoole6/metrics-accumulator
```
### Locally

**Build the project**
```bash
mvn clean package -DskipTests=true
```

**Start The Application**
```bash
java -jar target/app.jar --config-file ./metrics-accumulator.yml
```

Navigate to http://localhost:8080

**Pushing Data**

run the following command <u>twice</u>
```bash
curl -X 'POST' \
  'http://localhost:8080/update/default' \
  -H 'accept: */*' \
  -H 'X-API-KEY: 0d98f65f-074b-4d56-b834-576e15a3bfa5' \
  -H 'Content-Type: text/plain' \
  -d '# TYPE test_total counter
# HELP test_total
test_total {span_id="321",trace_id="123"} 5.0'
```

Then get the metric data
```bash
curl -X 'GET' \
  'http://localhost:8080/metrics/default' \
  -H 'accept: text/plain'
```

You should receive this
```text
# TYPE test_total counter
test_total{span_id="321",trace_id="123"} 10.0
```

You'll notice that `test_total` has a value of 10 

**Swagger**

You can run this example at the swagger endpoint http://localhost:8080/swagger-ui/index.html#/
## Supported Types

### Counters
Counters will be combined additively


### Gauges
Gauages are a special case. If you set the label `_metrics_accumulator_latest` to the current epoch time or any number. When a new gague value comes in for that time series it'll compare the label values of `_metrics_accumulator_latest` to the cached guage and the new one.

`_metrics_accumulator_latest` is not displayed when scraping.



## How to Utilize this service.
In the [metric-groups.yml](src%2Fmain%2Fresources%2Fprod%2Fmetric-groups.yml) file add your metric group

For example 
```yaml
global:
  restartCronExpression: "0 0 0 ? * *"
  hostAddress: localhost:8080

metricGroups:
  default:
    displayMetrics: true
    name: default
    maxTimeSeries: 2500
    apiKey: 0d98f65f-074b-4d56-b834-576e15a3bfa5
    restartCronExpression: "0 0 0 ? * *"
  metricGroup1:
    displayMetrics: true
    name: metric-group
    maxTimeSeries: 100
    apiKey: 0d98f65f-074b-4d56-b834-576e15a3bfa5
    restartCronExpression: "0 0 0 ? * *"  
```
### Configurations

#### Under Global

**restartCronExpression**

| Attributes            | Description                                                                                                                                                    |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| restartCronExpression | Used to set when your metrics are wiped from memory. This is useful getting rid of stale data. Ideally your data should be wiped at least once a week or more. | 
| hostAddress                      | This is the address of the metrics accumulator used for service discovery. For example, you might set up in DNS an A record pointing to the ip address of the instance running this service such as `metrics-accumulator.internal.com`.                                                                                                                                                               |

 

#### Under MetricGroups

| Attributes            | Description                                                                                                                                                                                                                                        |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| displayMetrics        | Determines if prometheus should read your metrics or not.                                                                                                                                                                                          |
| name                  | The name of the endpoint you'll push your metrics to.<br> For example if the `name` was *super-app* then the endpoint you'd need to POST your prometheus metrics is: `http://prometheus-metrics-accumulator.internal.q6cyber.com/update/super-app` |
| maxTimeSeries         | The maximum number of timeseries you can have in memory for your service. To deal with stale time series we have the *restartCronExpression* your metrics from history.                                                                            |
| apiKey                | The apikey must be passed in the header `X-API-KEY` when you POST your metrics. This helps to prevent an accidental POST to another metric Group or another environment such as prod/dev/qa.                                                       |
| restartCronExpression |Used to set when your metrics are wiped from memory. This is useful getting rid of stale data. Ideally your data should be wiped at least once a week or more. |


## Service Discovery

There's an example of service discovery via docker compose. In the root directory of the project run

```bash
mvn clean install -DskipTests=true
docker compose up --build --force-recreate
```

Wait about 30 seconds for the applications to start up.
Then navigate to http://localhost:9090/targets?search= and make sure your application appears  is visible. Follow the example [Getting Started](#getting-started)/pushing data to see metrics being consumed
### How Does It Work?


The endpoint `/service-discovery` returns a json structure that prometheus uses for [service discovery](https://prometheus.io/docs/prometheus/latest/http_sd/)

```json
[
  {
    "targets": [ "<hostAddress>"],
    "labels": {
      "__meta_metrics_path": "metrics/<metricGroup>",
    }
  },
  ...
]
```

In the Prometheus config you'll need to setup a scrape config for service discovery

```yaml
scrape_configs:
  # The job name is added as a label `job=<job_name>` to any time-series scraped from this config.
  - job_name: "metrics-accumulator"
    scrape_interval: 5s
    relabel_configs:
      - source_labels: ["__meta_metrics_path"]
        target_label: "__metrics_path__"
    http_sd_configs:
      - url: "http://localhost:8080/service-discovery"
```

Prometheus will query the service discovery endpoint `http://localhost:8080/service-discovery` and relabel will replace the metrics path with `/metrics/<metricGroup>`. A target is created for every metric as defined in your configuration file
