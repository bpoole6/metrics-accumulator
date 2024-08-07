# Metrics-Accumulator

The metric accumulator will accumulate metrics for ephemeral jobs. 

Counters will be combined additively


Gauages are a special case. If you set the label `_metric_consumer_latest` to the current epoch time only the latest value will be 


## Prometheus Scaper Respository
https://github.com/Q6Cyber/prometheus-scraper


## Environment Variables

**HOST_ADDRESS**

This should be the host address including port for Metrics Accumulator. For example in local development you should use 

`HOST_ADDRESS=localhost:8080`

This is used for the service discovery mechanism in Prometheus

## How to Utilize this service.
In the [metric-groups.yml](src%2Fmain%2Fresources%2Fprod%2Fmetric-groups.yml) file add your metric group

For example 
```yaml
metricGroups:
  botnet-sorter:
    displayMetrics: true
    name: botnet-sorter
    maxTimeSeries: 2500
    apiKey: 0d98f65f-074b-4d56-b834-576e15a3ffa5
    restartCronExpression: "0 0 0 ? * *"
```
### Configurations

**displayMetrics**

Determines if prometheus should read your metrics or not.

**name**

The name of the endpoint you'll push your metrics to.
For example if the `name` was *super-app* then the endpoint you'd need to POST your prometheus metrics to is 

```
http://prometheus-metrics-accumulator.internal.q6cyber.com/app/update/super-app
```

**maxTimeSeries**

The maximum number of timeseries you can have in memory for your service. To deal with stale time series we have the *restartCronExpression* your metrics from history. 

**apiKey**

The apikey must be passed in the header `X-API-KEY` when you POST your metrics. This helps to prevent an accidental POST to another metric Group or another environment such as prod/dev/qa. 

**restartCronExpression**

Used to set when your metrics are wiped from memory. This is useful getting rid of stale data. Ideally your data should be wiped at least once a week or more. 
