package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 将关联关系保存在db
 * @author xuelong.chen
 *
 */
public class ServiceLinkMetaData {
	
	private final List<ServiceLinkMetaData> serviceLinkMetaDatas = new ArrayList<>();
	private final Service service;

	public ServiceLinkMetaData(Service service) {
		this.service = service;
	}
	
	public Service getService() {
		return service;
	}
	
	public void addLinkMetaData(ServiceLinkMetaData metaData) {
		this.serviceLinkMetaDatas.add(metaData);
	}

	public List<ServiceLinkMetaData> getServiceLinkMetaDatas() {
		return serviceLinkMetaDatas;
	}
}
