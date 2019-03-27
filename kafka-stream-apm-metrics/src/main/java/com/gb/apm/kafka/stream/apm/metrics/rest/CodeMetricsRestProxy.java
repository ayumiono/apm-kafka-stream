package com.gb.apm.kafka.stream.apm.metrics.rest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.model.CodeMetrics;

@RestController
@RequestMapping("/code")
public class CodeMetricsRestProxy {
	
	private KafkaStreams streams;
	public CodeMetricsRestProxy(KafkaStreamApp app) {
		this.streams = app.streams();
	}
	
	@RequestMapping(value="/metrics/{systemId}", method = RequestMethod.GET)
	public List<CodeMetrics> get(@PathVariable("systemId") int systemId){
		ReadOnlyWindowStore<Integer, CodeMetrics> store = 
				streams.store(KafkaStreamApp.CODE_METRICS_STORE, QueryableStoreTypes.<Integer, CodeMetrics>windowStore());
		Map<Long, TimeWindow> windows =TimeWindows.of(60*60*1000).advanceBy(60*60*1000).windowsFor(System.currentTimeMillis());
		TimeWindow queryWindow = null;
		Iterator<Entry<Long, TimeWindow>> iteretor = windows.entrySet().iterator();
		while(iteretor.hasNext()) {
			queryWindow = iteretor.next().getValue();
			break;
		}
		WindowStoreIterator<CodeMetrics> methodMetrics = store.fetch(systemId, queryWindow.start(), queryWindow.end());
		
		List<CodeMetrics> result = new ArrayList<>();
		while(methodMetrics.hasNext()) {
			result.add(methodMetrics.next().value);
		}
		return result;
	}
}
