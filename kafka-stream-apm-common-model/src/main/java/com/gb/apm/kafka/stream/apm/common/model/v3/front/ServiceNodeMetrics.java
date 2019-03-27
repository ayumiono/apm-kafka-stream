package com.gb.apm.kafka.stream.apm.common.model.v3.front;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceMetrics;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceNodeV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.TimeBucket;
import com.gb.apm.model.TSpan;

public class ServiceNodeMetrics {
	private ServiceMetrics metrics;
	private List<ServiceMetrics> timebuckets;
	
	public ServiceNodeMetrics() {
		this.timebuckets = new ArrayList<>();
		this.metrics = null;
	}
	
	public ServiceMetrics getMetrics() {
		return metrics;
	}
	public void setMetrics(ServiceMetrics metrics) {
		this.metrics = metrics;
	}
	public List<ServiceMetrics> getTimebuckets() {
		return timebuckets;
	}
	public void setTimebuckets(List<ServiceMetrics> timebuckets) {
		this.timebuckets = timebuckets;
	}
	
	public static ServiceNodeMetrics build(ServiceNodeV2 serviceNode, long startMs, long endMs) {
		ServiceNodeMetrics t = new ServiceNodeMetrics();
		t.setMetrics(serviceNode.getMetrics());
		t.setTimebuckets(serviceNode.buckets_auto_merge(startMs, endMs).stream().map(new Function<TimeBucket<TSpan, ServiceMetrics>, ServiceMetrics>() {
			@Override
			public ServiceMetrics apply(TimeBucket<TSpan, ServiceMetrics> t) {
				return t.getMetrics();
			}
		}).collect(Collectors.toList()));
		return t;
	}
	
	public static void aggregate(ServiceNodeMetrics from ,ServiceNodeMetrics to) {
		ServiceMetrics.aggregate(from.getMetrics(), to.getMetrics());
		to.getTimebuckets().addAll(from.getTimebuckets());
	}
	
	public static void aggregate(ServiceNodeV2 serviceNode, long startMs, long endMs, ServiceNodeMetrics to) {
		ServiceNodeMetrics from  = ServiceNodeMetrics.build(serviceNode, startMs, endMs);
		ServiceNodeMetrics.aggregate(from, to);
	}
}
