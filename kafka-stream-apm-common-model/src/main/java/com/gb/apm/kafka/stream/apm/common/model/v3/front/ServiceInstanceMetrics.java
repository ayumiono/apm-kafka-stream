package com.gb.apm.kafka.stream.apm.common.model.v3.front;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceInstanceV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceMetrics;
import com.gb.apm.kafka.stream.apm.common.model.v3.TimeBucket;
import com.gb.apm.model.Annotation;
import com.gb.apm.model.TSpan;

/**
 * 
 * 前端展示数据模型
 * @author xuelong.chen
 *
 */
public class ServiceInstanceMetrics {
	private String endpoint;
	private List<Annotation> annotations;
	private ServiceMetrics metrics;
	private List<ServiceMetrics> timebuckets;
	
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	public List<Annotation> getAnnotations() {
		return annotations;
	}
	public void setAnnotations(List<Annotation> annotations) {
		this.annotations = annotations;
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
	
	public static ServiceInstanceMetrics build(ServiceInstanceV2 instance, long startMs, long endMs) {
		ServiceInstanceMetrics metrics = new ServiceInstanceMetrics();
		metrics.setAnnotations(instance.getAnnotations());
		metrics.setEndpoint(instance.getEndpoint());
		metrics.setMetrics(instance.getMetrics());
		List<ServiceMetrics> timebuckets = instance.buckets_auto_merge(startMs, endMs).stream().map(new Function<TimeBucket<TSpan, ServiceMetrics>, ServiceMetrics>() {
			@Override
			public ServiceMetrics apply(TimeBucket<TSpan, ServiceMetrics> t) {
				return t.getMetrics();
			}
		}).collect(Collectors.toList());
		metrics.setTimebuckets(timebuckets);
		return metrics;
	}
	
	public static void aggregate(ServiceInstanceMetrics from, ServiceInstanceMetrics to) {
		ServiceMetrics.aggregate(from.getMetrics(), to.getMetrics());
		to.getTimebuckets().addAll(from.getTimebuckets());
	}
	
	public static void aggregate(ServiceInstanceV2 instance,long startMs, long endMs, ServiceInstanceMetrics to) {
		ServiceInstanceMetrics.aggregate(ServiceInstanceMetrics.build(instance,startMs,endMs), to);
	}
}
