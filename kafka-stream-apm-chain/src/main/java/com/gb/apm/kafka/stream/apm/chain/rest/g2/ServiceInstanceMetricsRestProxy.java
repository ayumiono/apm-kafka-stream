package com.gb.apm.kafka.stream.apm.chain.rest.g2;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.websocket.server.PathParam;

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
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceInstanceV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceNodeV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.front.ServiceInstanceMetrics;
import com.gb.apm.kafka.stream.apm.common.utils.TimeWindowUtils;

@RestController
@RequestMapping("/service/instance")
@CrossOrigin
public class ServiceInstanceMetricsRestProxy {
	
	@Autowired
	private ChainAppV3 app;
	
	private List<ServiceInstanceMetrics> buildServiceInstanceMetrics(int servcieId, long startMs, long endMs, long timerange_start, long timerange_end) {
		Map<String, ServiceInstanceMetrics> instanceMetrics = new HashMap<>();
		ReadOnlyWindowStore<Integer, ServiceNodeV2> store = app.streams.store(KafkaStreamApp.CHAIN_METRICS_STORE, QueryableStoreTypes.windowStore());
		WindowStoreIterator<ServiceNodeV2> serviceNodes = store.fetch(servcieId, startMs, endMs);
		while(serviceNodes.hasNext()) {
			ServiceNodeV2 value = serviceNodes.next().value;
			Map<String, ServiceInstanceV2> instances =  value.getServerInstances();
			instances.entrySet().stream().forEach(new Consumer<Entry<String, ServiceInstanceV2>>() {
				@Override
				public void accept(Entry<String, ServiceInstanceV2> t) {
					if(instanceMetrics.containsKey(t.getKey())) {
						ServiceInstanceMetrics.aggregate(t.getValue(),timerange_start,timerange_end,instanceMetrics.get(t.getKey()));
					}else {
						instanceMetrics.put(t.getKey(), ServiceInstanceMetrics.build(t.getValue(),timerange_start,timerange_end));
					}
				}
			});
		}
		return new ArrayList<>(instanceMetrics.values());
	}
	
	@RequestMapping("metrics/{id}")
	public List<ServiceInstanceMetrics> metrics(@PathVariable("id") int id, @RequestBody Map<String, Object> requestPayload) throws ParseException {
		Long[] timeRange = TimeWindowUtils.extractRange(requestPayload);
		Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(KafkaStreamApp.ONEHOUR).advanceBy(KafkaStreamApp.ONEHOUR), timeRange[0], timeRange[1]);
		return this.buildServiceInstanceMetrics(id, fromTo[0], fromTo[1],timeRange[0],timeRange[1]);
	}
}
