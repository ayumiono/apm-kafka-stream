package com.gb.apm.kafka.stream.apm.chain.rest.grafana;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.gb.apm.kafka.stream.apm.chain.stream.ChainAppV3;
import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.grafana.TimeSeriesData;
import com.gb.apm.kafka.stream.apm.common.model.ChainMetricsV2;
import com.gb.apm.kafka.stream.apm.common.model.TimeBucket;
import com.gb.apm.kafka.stream.apm.common.model.utils.MetricsReporter;
import com.gb.apm.kafka.stream.apm.common.rest.RestExceptionMapper.CustomRestAPIException;
import com.gb.apm.kafka.stream.apm.common.utils.TimeWindowUtils;

//@RestController
//@RequestMapping("/grafana/query")
//@CrossOrigin
public class ChainMetricsTimeSeriesRestProxy {
	
	private static final Timer timeBucket = MetricsReporter.timer(MetricRegistry.name(ChainMetricsTimeSeriesRestProxy.class, "timeBucket"));
	
	@Autowired
	private ChainAppV3 app;
	
	@RequestMapping("/timebucket/{chainId}")
	public List<TimeSeriesData> timeBucket(@PathVariable("chainId") long chainId,
			@RequestBody Map<String, Object> requestPayload) throws ParseException {
		Timer.Context context = timeBucket.time();
		try {
			Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
			Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(KafkaStreamApp.ONEHOUR).advanceBy(KafkaStreamApp.ONEHOUR), timeRange[0], timeRange[1]);
			
			List<TimeBucket> result = new ArrayList<>();
			try {
				ReadOnlyWindowStore<Long, ChainMetricsV2> store = 
						app.streams.store(KafkaStreamApp.CHAIN_METRICS_STORE, QueryableStoreTypes.windowStore());
				WindowStoreIterator<ChainMetricsV2> methodMetrics = store.fetch(chainId, fromTo[0], fromTo[1]);
				while(methodMetrics.hasNext()) {
					result.addAll(methodMetrics.next().value.buckets(timeRange[0], timeRange[1]));
				}
			} catch (Exception e) {
				throw new CustomRestAPIException(CustomRestAPIException.UNKNOWN_ERROR_CODE,CustomRestAPIException.UNKNOW_ERROR_MSG);
			}
			return TimeSeriesData.wrapTimebucketData(result);
		} finally  {
			context.stop();
		}
	}
	
	@RequestMapping("/average/{chainId}")
	public List<TimeSeriesData> average(@PathVariable("chainId") long chainId,
			@RequestBody Map<String, Object> requestPayload) throws ParseException {
		Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
		Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(KafkaStreamApp.ONEHOUR).advanceBy(KafkaStreamApp.ONEHOUR), timeRange[0], timeRange[1]);
		ReadOnlyWindowStore<Long, ChainMetricsV2> store = app.streams.store(KafkaStreamApp.CHAIN_METRICS_STORE, QueryableStoreTypes.windowStore());
		WindowStoreIterator<ChainMetricsV2> methodMetrics = store.fetch(chainId, fromTo[0], fromTo[1]);
		TimeSeriesData seriesData = new TimeSeriesData();
		seriesData.setTarget("average");
		while(methodMetrics.hasNext()) {
			ChainMetricsV2 value = methodMetrics.next().value;
			if(value != null) {
				seriesData.addDatapoint(new Object[] {value.getAverageSpent(),value.getStartMs()});
			}
		}
		return Collections.singletonList(seriesData);
	}
	
	@RequestMapping("/invoke/{chainId}")
	public List<TimeSeriesData> invoke(@PathVariable("chainId") long chainId,
			@RequestBody Map<String, Object> requestPayload) throws ParseException {
		Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
		Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(KafkaStreamApp.ONEHOUR).advanceBy(KafkaStreamApp.ONEHOUR), timeRange[0], timeRange[1]);
		ReadOnlyWindowStore<Long, ChainMetricsV2> store = app.streams.store(KafkaStreamApp.CHAIN_METRICS_STORE, QueryableStoreTypes.windowStore());
		WindowStoreIterator<ChainMetricsV2> methodMetrics = store.fetch(chainId, fromTo[0], fromTo[1]);
		TimeSeriesData seriesData = new TimeSeriesData();
		seriesData.setTarget("invoke");
		while(methodMetrics.hasNext()) {
			ChainMetricsV2 value = methodMetrics.next().value;
			if(value != null) {
				seriesData.addDatapoint(new Object[] {value.getInvokeCount(),value.getStartMs()});
			}
		}
		return Collections.singletonList(seriesData);
	}
	
	@RequestMapping("/fail/{chainId}")
	public List<TimeSeriesData> fail(@PathVariable("chainId") long chainId,
			@RequestBody Map<String, Object> requestPayload) throws ParseException {
		Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
		Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(KafkaStreamApp.ONEHOUR).advanceBy(KafkaStreamApp.ONEHOUR), timeRange[0], timeRange[1]);
		ReadOnlyWindowStore<Long, ChainMetricsV2> store = app.streams.store(KafkaStreamApp.CHAIN_METRICS_STORE, QueryableStoreTypes.windowStore());
		WindowStoreIterator<ChainMetricsV2> methodMetrics = store.fetch(chainId, fromTo[0], fromTo[1]);
		TimeSeriesData seriesData = new TimeSeriesData();
		seriesData.setTarget("fail");
		while(methodMetrics.hasNext()) {
			ChainMetricsV2 value = methodMetrics.next().value;
			if(value != null) {
				seriesData.addDatapoint(new Object[] {value.getFailCount(),value.getStartMs()});
			}
		}
		return Collections.singletonList(seriesData);
	}
	
	@RequestMapping("/timeout/{chainId}")
	public List<TimeSeriesData> timeout(@PathVariable("chainId") long chainId,
			@RequestBody Map<String, Object> requestPayload) throws ParseException {
		Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
		Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(KafkaStreamApp.ONEHOUR).advanceBy(KafkaStreamApp.ONEHOUR), timeRange[0], timeRange[1]);
		ReadOnlyWindowStore<Long, ChainMetricsV2> store = app.streams.store(KafkaStreamApp.CHAIN_METRICS_STORE, QueryableStoreTypes.windowStore());
		WindowStoreIterator<ChainMetricsV2> methodMetrics = store.fetch(chainId, fromTo[0], fromTo[1]);
		TimeSeriesData seriesData = new TimeSeriesData();
		seriesData.setTarget("timeout");
		while(methodMetrics.hasNext()) {
			ChainMetricsV2 value = methodMetrics.next().value;
			if(value != null) {
				seriesData.addDatapoint(new Object[] {value.getTimeoutCount(),value.getStartMs()});
			}
		}
		return Collections.singletonList(seriesData);
	}
}
