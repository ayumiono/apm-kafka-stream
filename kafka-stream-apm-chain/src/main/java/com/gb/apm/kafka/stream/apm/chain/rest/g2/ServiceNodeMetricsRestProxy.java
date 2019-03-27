package com.gb.apm.kafka.stream.apm.chain.rest.g2;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

import com.gb.apm.kafka.stream.apm.chain.stream.ChainAppV3;
import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceMetrics;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceNodeV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.TimeBucket;
import com.gb.apm.kafka.stream.apm.common.model.v3.front.ServiceNodeMetrics;
import com.gb.apm.kafka.stream.apm.common.utils.TimeWindowUtils;
import com.gb.apm.model.TSpan;

@RestController
@RequestMapping("/service/node")
@CrossOrigin
public class ServiceNodeMetricsRestProxy {
	
	@Autowired
	private ChainAppV3 app;
	
	@RequestMapping("/timebuckets/{serviceId}")
	public List<ServiceMetrics> timebuckets(@PathVariable("serviceId") int serviceId,
			@RequestBody Map<String, Object> requestPayload) throws ParseException {
		Long[] timeRange = TimeWindowUtils.extractRange(requestPayload);
		Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(KafkaStreamApp.ONEHOUR).advanceBy(KafkaStreamApp.ONEHOUR), timeRange[0], timeRange[1]);
		long startMs = fromTo[0], endMs = fromTo[1];
		ReadOnlyWindowStore<Integer, ServiceNodeV2> store = app.streams.store(KafkaStreamApp.CHAIN_METRICS_STORE, QueryableStoreTypes.windowStore());
		WindowStoreIterator<ServiceNodeV2> serviceNodes = store.fetch(serviceId, startMs, endMs);
		List<ServiceMetrics> res = new ArrayList<>();
		while(serviceNodes.hasNext()) {
			ServiceNodeV2 value = serviceNodes.next().value;
			res.addAll(value.buckets_auto_merge(timeRange[0], timeRange[1]).stream().map(new Function<TimeBucket<TSpan, ServiceMetrics>, ServiceMetrics>() {
				@Override
				public ServiceMetrics apply(TimeBucket<TSpan, ServiceMetrics> t) {
					return t.getMetrics();
				}
			}).collect(Collectors.toList()));
		}
		return res;
	}
	
	@RequestMapping("/metrics/{serviceId}")
	public ServiceNodeMetrics metrics(@PathVariable("serviceId") int serviceId,
			@RequestBody Map<String, Object> requestPayload) throws ParseException {
		Long[] timeRange = TimeWindowUtils.extractRange(requestPayload);
		Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(KafkaStreamApp.ONEHOUR).advanceBy(KafkaStreamApp.ONEHOUR), timeRange[0], timeRange[1]);
		long startMs = fromTo[0], endMs = fromTo[1];
		ReadOnlyWindowStore<Integer, ServiceNodeV2> store = app.streams.store(KafkaStreamApp.CHAIN_METRICS_STORE, QueryableStoreTypes.windowStore());
		WindowStoreIterator<ServiceNodeV2> serviceNodes = store.fetch(serviceId, startMs, endMs);
		ServiceNodeMetrics metrics = null;
		while(serviceNodes.hasNext()) {
			ServiceNodeV2 value = serviceNodes.next().value;
			if(metrics != null) {
				ServiceNodeMetrics.aggregate(value, timeRange[0], timeRange[1], metrics);
			}else {
				metrics = ServiceNodeMetrics.build(value, timeRange[0], timeRange[1]);
			}
		}
		return metrics == null ? new ServiceNodeMetrics() : metrics;
	}
}
