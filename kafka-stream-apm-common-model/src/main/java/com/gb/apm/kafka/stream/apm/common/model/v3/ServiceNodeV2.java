package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.streams.kstream.TimeWindows;

import com.gb.apm.kafka.stream.apm.common.model.utils.JacksonUtils;
import com.gb.apm.model.Annotation;
import com.gb.apm.model.TSpan;
import com.gb.apm.plugins.dubbo.DubboConstants;

public class ServiceNodeV2 extends AbstractMetricObject<TSpan, ServiceMetrics, ServiceMetrics> {

	private static final long serialVersionUID = -8770113601361824824L;

	private Map<String, ServiceInstanceV2> serverInstances = new HashMap<>();
	private final Service service;

	public ServiceNodeV2(Service service, long startMs, long endMs) {
		super(startMs, endMs);
		this.service = service;
	}

	public ServiceNodeV2(Service service, long startMs, long endMs, TimeWindows timeWindows,
			ServiceMetrics metrics) {
		super(startMs, endMs, metrics, timeWindows);
		this.service = service;
	}

	@Override
	public void doCustomLogic(TSpan frame) {
		String address = frame.getEndPoint();
		if (address == null) {// 兼容USER serviceType
			address = "local";
		}
		List<Annotation> annotations = new ArrayList<>();
		if (frame.getServiceType() == DubboConstants.DUBBO_PROVIDER_SERVICE_TYPE.getCode()) {
			annotations.add(frame.annotationFor(DubboConstants.DUBBO_LOAD_LIMIT.getCode()));
			annotations.add(frame.annotationFor(DubboConstants.DUBBO_GROUP.getCode()));
			annotations.add(frame.annotationFor(DubboConstants.DUBBO_VERSION.getCode()));
		}
		if (!serverInstances.containsKey(address)) {
			serverInstances.put(address, new ServiceInstanceV2(this.getStartMs(), this.getEndMs(), frame.getEndPoint(), annotations));
		}
		serverInstances.get(address).hit(frame, frame.getStartTime());
	}

	@Override
	public ServiceMetrics initMetrics(long startMs) {
		ServiceMetrics m = new ServiceMetrics();
		m.setStartMs(startMs);
		return m;
	}

	@Override
	public ServiceMetrics initTimeBucketMetrics(long startMs) {
		ServiceMetrics m = new ServiceMetrics();
		m.setStartMs(startMs);
		return m;
	}

	@Override
	public TimeWindows initTimeWindows() {
		return TimeWindows.of(5 * 60 * 1000).advanceBy(5 * 60 * 1000);// FIXME
	}

	public Map<String, ServiceInstanceV2> getServerInstances() {
		return serverInstances;
	}

	public void setServerInstances(Map<String, ServiceInstanceV2> serverInstances) {
		this.serverInstances = serverInstances;
	}

	public Service getService() {
		return service;
	}

	@Override
	public String toString() {
		return JacksonUtils.toJson(this);
//		return "ServiceNodeV2 [service=" + service + ", serverInstances=" + serverInstances + ", metrics="
//				+ this.getMetrics() + ", timebuckets=" + this.getTimeBuckets() + ", timewindows="
//				+ this.getTimeBucketWindow() + "]";
	}
}
