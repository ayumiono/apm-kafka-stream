package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.util.List;

import org.apache.kafka.streams.kstream.TimeWindows;

import com.gb.apm.kafka.stream.apm.common.model.utils.JacksonUtils;
import com.gb.apm.model.Annotation;
import com.gb.apm.model.TSpan;

public class ServiceInstanceV2 extends AbstractMetricObject<TSpan, ServiceMetrics, ServiceMetrics> {

	private static final long serialVersionUID = -2171973914426981170L;

	private final String endpoint;// host:port

	private final List<Annotation> annotations;

	public ServiceInstanceV2(long startMs, long endMs, TimeWindows timeWindows, ServiceMetrics metrics,
			String endpoint, List<Annotation> annotations) {
		super(startMs, endMs, metrics, timeWindows);
		this.endpoint = endpoint;
		this.annotations = annotations;
	}

	public ServiceInstanceV2(long startMs, long endMs, String endpoint, List<Annotation> annotations) {
		super(startMs, endMs);
		this.endpoint = endpoint;
		this.annotations = annotations;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	@Override
	public ServiceMetrics initMetrics(long startMs) {
		ServiceMetrics m = new ServiceMetrics();
		m.setStartMs(startMs);
		return m;
	}

	@Override
	public TimeWindows initTimeWindows() {
		return TimeWindows.of(5 * 60 * 1000).advanceBy(5 * 60 * 1000);// FIXME
	}

	@Override
	public ServiceMetrics initTimeBucketMetrics(long startMs) {
		ServiceMetrics m = new ServiceMetrics();
		m.setStartMs(startMs);
		return m;
	}

	@Override
	public void doCustomLogic(TSpan t) {
	}

	@Override
	public String toString() {
		return JacksonUtils.toJson(this);
	}
}
