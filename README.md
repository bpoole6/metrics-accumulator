<h1 align="center" style="border-bottom: none">
   <img alt="Metrics Accumulator" src="/documentation/images/logo.svg" width="600"><br>Metrics Accumulator
</h1>

<!-- TOC -->
  * [Description](#description)
  * [Features](#features)
  * [Program Arguments](#program-arguments)
  * [API](#api)
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
  * [Metrics Accumulator Clients](#metrics-accumulator-clients)
    * [Python](#python)
    * [Nodejs](#nodejs)
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

## API
| Api Endpoint                      | Method | Required Headers | Description                                                                                                                          |
|-----------------------------------|--------|------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| /reset-metric-group/{metricGroup} | PUT    | N/A              | Erases the metric group metrics out of memory.                                                                                       |
| /reload-configuration             | PUT    | N/A              | Reloads configuration that were passed in via --config-file. The File source will be reread from storage. All Metrics will be erased |
| /update/{metricGroup}             | POST   | X-API-KEY        | Updates metric group metrics.                                                                                                        |
| /service-discovery             | GET    | N/A              | A service discovery mechanism for prometheus Please see documentation https://prometheus.io/docs/prometheus/latest/http_sd/          |
| /metrics/{metricGroup}            | GET    | N/A              | Returns the metrics for a metrics group.                                                                                             |
| /current-configurations            | GET    | N/A              | Displays the current loaded configurations                                                                                           |
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
Gauges are a special case. When a new gauge value comes then the last guage value to be added will be preserved. To change this behavior you can set the label `_metrics_accumulator_latest` with the value of a number such as the epoch time. Subsequent pushes on the same gauge will compare the vlaue of  `_metrics_accumulator_latest` and largest number will be persisted.

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
    serviceDiscoveryLabels:
      env: qa
      version: v31
  metricGroup1:
    displayMetrics: true
    name: metric-group
    maxTimeSeries: 100
    apiKey: 0d98f65f-074b-4d56-b834-576e15a3bfa5
    restartCronExpression: "0 0 0 ? * *"
    serviceDiscoveryLabels:
      env: test
      version: v45 
```
### Configurations

#### Under Global

**restartCronExpression**

| Attributes            | Description                                                                                                                                                    |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| restartCronExpression | Used to set when your metrics are wiped from memory. This is useful getting rid of stale data. Ideally your data should be wiped at least once a week or more. | 
| hostAddress                      | This is the address of the metrics accumulator used for service discovery. For example, you might set up in DNS an A record pointing to the ip address of the instance running this service such as `metrics-accumulator.internal.com`.                                                                                                                                                               |

 

#### Under MetricGroups

| Attributes            | Description                                                                                                                                                                                                                                       |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| displayMetrics        | Determines if prometheus should read your metrics or not.                                                                                                                                                                                         |
| name                  | The name of the endpoint you'll push your metrics to.<br> For example if the `name` was *super-app* then the endpoint you'd need to POST your prometheus metrics is: `http://prometheus-metrics-accumulator.internal.q6cyber.com/update/super-app` |
| maxTimeSeries         | The maximum number of timeseries you can have in memory for your service. To deal with stale time series we have the *restartCronExpression* your metrics from history.                                                                           |
| apiKey                | The apikey must be passed in the header `X-API-KEY` when you POST your metrics. This helps to prevent an accidental POST to another metric Group or another environment such as prod/dev/qa.                                                      |
| restartCronExpression | Used to set when your metrics are wiped from memory. This is useful getting rid of stale data. Ideally your data should be wiped at least once a week or more.                                                                                    |
| serviceDiscoveryLabels | Additional labels to be added when scraping the serviceDiscovery endpoint. This is useful during the relabeling                                                                                                                                   |


## Service Discovery

There's an example of service discovery via docker compose. In the root directory of the project.

```bash
mvn clean install -DskipTests=true
docker compose up --build --force-recreate
```

Then navigate to http://localhost:9090/targets?search= and make sure your applicationis being scraped. Follow the example [Getting Started](#getting-started)/pushing data to see metrics being consumed
### How Does It Work?


The endpoint `/service-discovery` returns a json structure that prometheus uses for [service discovery](https://prometheus.io/docs/prometheus/latest/http_sd/)

```json
[
  {
    "targets": [ "<hostAddress>"],
    "labels": {
      "__meta_metrics_path": "metrics/<metricGroup>"
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

## Metrics Accumulator Clients

There's full client support 
- [python](https://pypi.org/project/metrics-accumulator-client/)
- [nodejs](https://www.npmjs.com/package/metrics-accumulator-client)

There is a java example of a client found here
- https://github.com/bpoole6/metrics-accumulator-clients/tree/main/java-client
### Python

**Installation**
```bash
python -m pip install metrics-accumulator-client
```

Example
```python
from Client import Client
from prometheus_client import Counter,Gauge, CollectorRegistry, metrics
metrics.disable_created_metrics() #*****Important****** If you don't set this then metrics accumulator will Amber Heard the bed
registry = CollectorRegistry()
c = Counter("hello_total", "dock", labelnames=['application'], registry=registry)
c.labels(["app"]).inc()

g = Gauge("man", "dock", labelnames=['application'], registry=registry)
g.labels(["app"]).inc()

client = Client("http://localhost:8080", "0d98f65f-074b-4d56-b834-576e15a3bfa5")
client.update_metrics("default", registry)
print(client.get_metric_group("default").content.decode())
print(client.reload_configurations().status_code)
print(client.reset_metric_group("default").status_code)
print(client.service_discovery().status_code)
print(client.current_configurations().status_code)
```

### Nodejs

**Installation**
```bash
npm install metrics-accumulator-client
```

Example 

```node
import {Registry, Counter} from "prom-client"

const registry = new Registry()
new Counter({
    name : "counter_example_total",
    help: "help",
    registers: [registry]
})

let client = new Client("http://localhost:8080", "0d98f65f-074b-4d56-b834-576e15a3bfa5")
client.updateMetrics('default', registry).then(res=> console.log(res.statusCode + " " + res.content))
client.getMetricGroup('default').then(res=> console.log(res.statusCode + " " + res.content))
client.reloadConfigurations().then(res=> console.log(res.statusCode + " " + res.content))
client.resetMetricGroup("default").then(res=> console.log(res.statusCode + " " + res.content))
client.serviceDiscovery().then(res=> console.log(res.statusCode + " " + res.content))
client.currentConfigurations().then(res=> console.log(res.statusCode + " " + res.content))
```