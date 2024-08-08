package io.bpoole6.accumulator;

import io.bpoole6.accumulator.service.MetricService;
import io.bpoole6.accumulator.service.RegistryRepository;
import io.bpoole6.accumulator.service.metricgroup.Group;
import io.bpoole6.accumulator.service.MetricsAccumulatorConfiguration;
import io.bpoole6.accumulator.util.Utils;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import prometheus.types.Counter;
import prometheus.types.Gauge;
import prometheus.types.Metric;
import prometheus.types.MetricFamily;
import prometheus.types.MetricType;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD) //In the future we may want to just reset the registries and maps between tests. Reloading the context may become expensive
class MetricsConsumerApplicationTests{


	@Value("classpath:data/default/metrics")
	private Resource metrics1;

	@Value("classpath:data/label_timestamp/metrics-future")
	private Resource metricsFuture;

	@Value("classpath:data/label_timestamp/metrics-old")
	private Resource metricsOld;

	@Autowired
	private RegistryRepository registryRepository;

	@Autowired
	private MetricService metricService;

	private Group defaultGroup;
	@Autowired
	private MetricsAccumulatorConfiguration metricsAccumulatorConfiguration;

	public MetricsConsumerApplicationTests() {

	}
	
	@BeforeEach
	void setup() {
		this.defaultGroup = registryRepository.getRegistryMap().keySet().stream().findFirst().get();
	}

	@Test
	void testCounterAccumulator() throws IOException, InterruptedException {
		List<MetricFamily> originalMetrics = Utils.readMetrics(
				new String(metrics1.getContentAsByteArray()));
		metricService.modifyMetrics(new String(metrics1.getContentAsByteArray()), defaultGroup);
		metricService.modifyMetrics(new String(metrics1.getContentAsByteArray()), defaultGroup);

		PrometheusMeterRegistry prometheusRegistry =this.registryRepository.getRegistry(defaultGroup).getPrometheusRegistry();
		String snapshot = prometheusRegistry.scrape();
		List<MetricFamily> newMetrics =Utils.readMetrics(snapshot);

		evalMetricFamilies(originalMetrics, newMetrics);

	}

	@Test
	void testGaugeTimestamp() throws IOException, InterruptedException {
		String olderMetrics = metricsOld.getContentAsString(Charset.defaultCharset());
		String newerMetrics = metricsFuture.getContentAsString(Charset.defaultCharset());

		this.metricService.modifyMetrics(olderMetrics, defaultGroup);
		this.metricService.modifyMetrics(newerMetrics, defaultGroup);

		Gauge oldGauge = (Gauge) Utils.readMetrics(olderMetrics).get(0).getMetrics().get(0);
		Gauge newGauge = (Gauge) Utils.readMetrics(this.registryRepository.getRegistry(defaultGroup).getPrometheusRegistry().scrape()).get(0).getMetrics().get(0);
		Assertions.assertNotSame(oldGauge.getValue(), newGauge.getValue());
		Assertions.assertEquals(4911.0, newGauge.getValue());
	}

	@Test
	void testMaxMetrics() throws IOException, InterruptedException {
		String formatted = """
				# HELP python%s_gc_objects_collected_seconds Objects collected during gc
				# TYPE python%s_gc_objects_collected_seconds gauge
				python%s_gc_objects_collected_seconds{generation="0",_metrics_accumulator_latest="3992623250"} 4911.0
				""";
		String strings = "";
		for(int i = 0; i < 150; i++) {
			strings += formatted.formatted(i,i,i);
		}
		this.metricService.modifyMetrics(strings, defaultGroup);
		Assertions.assertEquals(defaultGroup.getMaxTimeSeries(), this.registryRepository.getRegistry(defaultGroup).getPrometheusRegistry().getPrometheusRegistry().scrape().size());
	}



	public void evalMetricFamilies(List<MetricFamily> originalMetrics, List<MetricFamily> newMetrics) throws IOException {
		for (MetricFamily metricFamily : originalMetrics) {
			Optional<MetricFamily> opt = newMetrics.stream()
					.filter(i -> i.getName().equals(metricFamily.getName())).findFirst();
			if(opt.isEmpty()){
				Assertions.fail("Missing MetricFamily  "+ metricFamily.getName());
			}
			MetricFamily foundMetricFamily = opt.get();
			Assertions.assertEquals(metricFamily.getType(), foundMetricFamily.getType());
			for(Metric metric : metricFamily.getMetrics()){
				Metric foundMetric = foundMetricFamily.getMetrics().stream().filter(i-> i.getName().equals(metric.getName()) && i.getLabels().equals(metric.getLabels())).findFirst().get();
				Assertions.assertEquals(metric.getLabels().keySet(), foundMetric.getLabels().keySet());
				if(metricFamily.getType() == MetricType.COUNTER) {
					Counter c = ((prometheus.types.Counter)metric);
					Counter foundC = ((prometheus.types.Counter)foundMetric);
					Assertions.assertEquals(c.getValue()*2, foundC.getValue() );
				}else if(metricFamily.getType() == MetricType.GAUGE){
					Gauge g = ((prometheus.types.Gauge)metric);
					Gauge foundG = ((prometheus.types.Gauge)foundMetric);
					Assertions.assertEquals(g.getValue(), foundG.getValue() );
				}

			}
		}

	}
}
