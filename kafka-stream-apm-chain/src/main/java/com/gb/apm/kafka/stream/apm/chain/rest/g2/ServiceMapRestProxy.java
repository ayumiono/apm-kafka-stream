package com.gb.apm.kafka.stream.apm.chain.rest.g2;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gb.apm.kafka.stream.apm.chain.stream.ChainAppV3;
import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.dao.GBPersistenceException;
import com.gb.apm.kafka.stream.apm.common.dao.ServiceDao;
import com.gb.apm.kafka.stream.apm.common.dao.ServiceLinkDao;
import com.gb.apm.kafka.stream.apm.common.model.v3.Service;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceLink;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceMetrics;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceNodeV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.front.ServiceMapEdge;
import com.gb.apm.kafka.stream.apm.common.model.v3.front.ServiceMapNode;
import com.gb.apm.kafka.stream.apm.common.utils.TimeWindowUtils;

@RestController
@RequestMapping("/service/map")
@CrossOrigin
public class ServiceMapRestProxy {
	@Autowired
	private ChainAppV3 app;
	
	@Autowired
	private ServiceDao serviceDao;
	
	@Autowired
	private ServiceLinkDao serviceLinkDao;
	
	private ServiceMapNode buildServiceMapNode(Service service, long startMs, long endMs) {
		int invokeCount = 0;
		int failCount = 0;
		ReadOnlyWindowStore<Integer, ServiceNodeV2> store = app.streams.store(KafkaStreamApp.CHAIN_METRICS_STORE, QueryableStoreTypes.windowStore());
		WindowStoreIterator<ServiceNodeV2> serviceNodes = store.fetch(service.getId(), startMs, endMs);
		while(serviceNodes.hasNext()) {
			ServiceNodeV2 value = serviceNodes.next().value;
			if(value != null) {
				ServiceMetrics metrics = value.getMetrics();
				invokeCount += metrics.getInvokeCount();
				failCount += metrics.getFailCount();
			}
		}
		ServiceMapNode node = ServiceMapNode.build(service);
		node.setInvokeCount(invokeCount);
		node.setFailCount(failCount);
		return node;
	}
	
	@RequestMapping("/data")
	public Map<String, List> data(@RequestBody Map<String, Object> requestPayload) throws ParseException, GBPersistenceException {
		Long[] timeRange = TimeWindowUtils.extractTimeRange(requestPayload);
		Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(KafkaStreamApp.ONEHOUR).advanceBy(KafkaStreamApp.ONEHOUR), timeRange[0], timeRange[1]);
		long startMs = fromTo[0];
		long endMs = fromTo[1];
		Map<String, List> result = new HashMap<>();/*{nodes:[],edges:[]}*/
		List<ServiceMapEdge> edges = new ArrayList<>();
		List<ServiceMapNode> nodes = new ArrayList<>();
		List<Service> services = serviceDao.getServices();
		Set<Integer> existServiceIds = services.stream().map(new Function<Service, Integer>() {
			@Override
			public Integer apply(Service t) {
				return t.getId();
			}
		}).collect(Collectors.toSet());
		for(Service service : services) {
			nodes.add(buildServiceMapNode(service, startMs, endMs));
			int id = service.getId();
			ServiceLink links = serviceLinkDao.findServiceLinks(id);
			if(links == null) continue;
			Set<Integer> targets = links.getTargetKeys();
			for(Integer target : targets) {
				if(!existServiceIds.contains(target)) continue;
				String edgeId = id+"-"+target;
				ServiceMapEdge edge = new ServiceMapEdge();
				edge.setId(edgeId);
				edge.setSource(id);
				edge.setTarget(target);
				edges.add(edge);
			}
		}
		result.put("nodes", nodes);
		result.put("edges", edges);
		return result;
	}
	
}
