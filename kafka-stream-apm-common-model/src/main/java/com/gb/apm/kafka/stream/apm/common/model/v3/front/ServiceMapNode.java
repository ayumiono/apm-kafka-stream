package com.gb.apm.kafka.stream.apm.common.model.v3.front;

import com.gb.apm.kafka.stream.apm.common.model.v3.Service;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceURL;

public class ServiceMapNode {
	private String agentId;
	private String applicationName;
	private int serviceTypeCode;
	private String serviceUrl;
	private int id;
	private int invokeCount;
	private int failCount;
	public String getAgentId() {
		return agentId;
	}
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	public String getApplicationName() {
		return applicationName;
	}
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	public int getServiceTypeCode() {
		return serviceTypeCode;
	}
	public void setServiceTypeCode(int serviceTypeCode) {
		this.serviceTypeCode = serviceTypeCode;
	}
	public String getServiceUrl() {
		return serviceUrl;
	}
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getInvokeCount() {
		return invokeCount;
	}
	public void setInvokeCount(int invokeCount) {
		this.invokeCount = invokeCount;
	}
	public int getFailCount() {
		return failCount;
	}
	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}
	
	
	public static ServiceMapNode build(Service service) {
		ServiceMapNode node = new ServiceMapNode();
		node.setAgentId(service.getAgentId());
		node.setId(service.getId());
		node.setServiceTypeCode(service.getServiceTypeCode());
		node.setServiceUrl(ServiceURL.valueOf(service.getServiceUrl()).getPath());
		node.setApplicationName(service.getApplicationName());
		return node;
	}
}
