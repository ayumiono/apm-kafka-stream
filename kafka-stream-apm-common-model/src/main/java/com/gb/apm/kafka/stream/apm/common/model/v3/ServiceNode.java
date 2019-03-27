package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gb.apm.model.Annotation;
import com.gb.apm.model.TSpan;
import com.gb.apm.plugins.dubbo.DubboConstants;

/**
 * 逻辑应用分组，对应可能的多个应用实例
 * 实时分析
 * provider side
 * @author xuelong.chen
 */
@Deprecated
public class ServiceNode {

	private Map<String, ServiceInstance> serverInstances = new HashMap<>();

	private Service service;
	
	public ServiceNode() {}
	
	private long startMs;
	private long endMs;
	

	public ServiceNode(Service service) {
		this.service = service;
	}

	public ServiceNode hit(TSpan frame) {
		String address = frame.getEndPoint();
		if(address == null) {//兼容USER serviceType
			address = "local";
		}
		List<Annotation> annotations = new ArrayList<>();
		if(frame.getServiceType() == DubboConstants.DUBBO_PROVIDER_SERVICE_TYPE.getCode()) {
			annotations.add(frame.annotationFor(DubboConstants.DUBBO_LOAD_LIMIT.getCode()));
			annotations.add(frame.annotationFor(DubboConstants.DUBBO_GROUP.getCode()));
			annotations.add(frame.annotationFor(DubboConstants.DUBBO_VERSION.getCode()));
		}
		if (!serverInstances.containsKey(address)) {
			serverInstances.put(address, ServiceInstance.timebucket(service, frame.getEndPoint(),annotations, 5*60*1000));//FIXME
		}
		serverInstances.get(address).hit(frame);
		return this;
	}

	public Service getService() {
		return service;
	}

	public Map<String, ServiceInstance> getServerInstances() {
		return serverInstances;
	}

	@Override
	public int hashCode() {
		return service.getId();
	}
	
	public void setServerInstances(Map<String, ServiceInstance> serverInstances) {
		this.serverInstances = serverInstances;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public long getStartMs() {
		return startMs;
	}

	public void setStartMs(long startMs) {
		this.startMs = startMs;
	}

	public long getEndMs() {
		return endMs;
	}

	public void setEndMs(long endMs) {
		this.endMs = endMs;
	}
	
	@Override
	public String toString() {
		StringBuilder sbuilder = new StringBuilder("ServiceNode [ service=");
		sbuilder.append(service.getServiceUrl());
		if(serverInstances != null && serverInstances.size() > 0) {
			sbuilder.append(", instances={");
			for(String host : serverInstances.keySet()) {
				sbuilder.append(host).append(" ");
			}
			sbuilder.append("}");
		}
		
		sbuilder.append("]");
		return sbuilder.toString();
	}
}
