package com.gb.apm.kafka.stream.apm.metrics.rest.grafana;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.grafana.TimeSeriesData;
import com.gb.apm.kafka.stream.apm.common.model.CodeMetrics;
import com.gb.apm.kafka.stream.apm.common.model.MethodMetricsV2;
import com.gb.apm.kafka.stream.apm.common.model.TimeBucket;
import com.gb.apm.kafka.stream.apm.common.model.utils.SHAHashUtils;
import com.gb.apm.kafka.stream.apm.common.utils.TimeWindowUtils;

@RestController
@RequestMapping("/grafana/query")
@CrossOrigin
public class MethodMetricsTimeSeriesRestProxy {
	
	private KafkaStreams streams;
	
	public MethodMetricsTimeSeriesRestProxy(KafkaStreamApp app) {
		this.streams = app.streams();
	}
	
	@RequestMapping(value="/code/{systemId}", method = RequestMethod.POST)
	public List<TimeSeriesData> metrics(@PathVariable int systemId, @RequestBody Map<String,Object> requestPayload) throws ParseException{
		
		Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
		Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(60*60*1000).advanceBy(60*60*1000), timeRange[0], timeRange[1]);
		ReadOnlyWindowStore<Integer, CodeMetrics> store = 
				streams.store(KafkaStreamApp.CODE_METRICS_STORE, QueryableStoreTypes.<Integer, CodeMetrics>windowStore());
		WindowStoreIterator<CodeMetrics> methodMetrics = store.fetch(systemId, fromTo[0], fromTo[1]);
		
		List<CodeMetrics> result = new ArrayList<>();
		while(methodMetrics.hasNext()) {
			result.add(methodMetrics.next().value);
		}
		return TimeSeriesData.wrapCodeMetrics(result);
	}
	
	@RequestMapping(value="/systemlevel/timebucket",method=RequestMethod.POST)
	public List<TimeSeriesData> grafanaSystemLevelTimeBucket(@RequestParam("methodName") String methodName,@RequestParam("systemId") int systemId
			,@RequestBody Map<String, Object> requestPayload) throws ParseException{
		
		Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
		Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(60*60*1000).advanceBy(60*60*1000), timeRange[0], timeRange[1]);
		
		long methodId = SHAHashUtils.unsignedLongHash(systemId,methodName);
		ReadOnlyWindowStore<Long, MethodMetricsV2> store = 
				streams.store(KafkaStreamApp.METHOD_INVOKE_METRICS_METHODLEVEL_STORE, QueryableStoreTypes.windowStore());
		
		WindowStoreIterator<MethodMetricsV2> methodMetrics = store.fetch(methodId, fromTo[0], fromTo[1]);
		
		List<TimeBucket> result = new ArrayList<>();
		while(methodMetrics.hasNext()) {
			result.addAll(methodMetrics.next().value.buckets(timeRange[0],timeRange[1]));
		}
		return TimeSeriesData.wrapTimebucketData(result);
	}
	
	@RequestMapping(value="/chainlevel/timebucket",method=RequestMethod.POST)
	public List<TimeSeriesData> grafanaChainLevelTimeBucket(@RequestParam("stackId") long stackId,@RequestBody Map<String, Object> requestPayload) throws ParseException{
		
		Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
		Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(60*60*1000).advanceBy(60*60*1000), timeRange[0], timeRange[1]);
		
		ReadOnlyWindowStore<Long, MethodMetricsV2> store = 
				streams.store(KafkaStreamApp.METHOD_INVOKE_METRICS_STORE, QueryableStoreTypes.windowStore());
		
		WindowStoreIterator<MethodMetricsV2> methodMetrics = store.fetch(stackId, fromTo[0], fromTo[1]);
		
		List<TimeBucket> result = new ArrayList<>();
		while(methodMetrics.hasNext()) {
			result.addAll(methodMetrics.next().value.buckets(timeRange[0],timeRange[1]));
		}
		return TimeSeriesData.wrapTimebucketData(result);
	}
}
