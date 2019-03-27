package com.gb.apm.kafka.stream.apm.common.model.v3;

import com.gb.apm.common.trace.ServiceType;
import com.gb.apm.kafka.stream.apm.common.annotation.MCollection;
import com.gb.apm.kafka.stream.apm.common.annotation.MID;

/**
 * 应用描述，可以是dubbo服务，spring cloud服务，存储服务等
 * 
 * @author xuelong.chen
 *
 */
@MCollection("apm_virtual_application_node")
public class Service {
	
	private final String agentId;
	
	private final String applicationName;
	
    private final short serviceTypeCode;
    
    private final String serviceType;
    
    private final String serviceUrl;
    
    @MID
    private final int id;
    
    public Service(String serviceURL, String serviceType, short serviceTypeCode, String agentId, String applicationName, int id) {
    	this.serviceType = serviceType;
    	this.serviceTypeCode = serviceTypeCode;
    	this.agentId = agentId;
    	this.id = id;
    	this.applicationName = applicationName;
    	this.serviceUrl = serviceURL;
    }
    
    public Service(ServiceURL serviceUrl, ServiceType serviceType, String agentId, String applicationName) {
        if (serviceUrl == null) {
            throw new NullPointerException("name must not be null. serviceType=" + serviceType);
        }
        if (serviceType == null) {
            throw new NullPointerException("serviceType must not be null. url=" + serviceUrl);
        }
        this.serviceUrl = serviceUrl.toIdentityString();
        this.serviceType = serviceType.getName();
        this.serviceTypeCode = serviceType.getCode();
        this.id = serviceUrl.hashCode();
        this.agentId = agentId;
        this.applicationName = applicationName;
    }

	public short getServiceTypeCode() {
		return serviceTypeCode;
	}

	public String getServiceType() {
		return serviceType;
	}

	public String getServiceUrl() {
		return serviceUrl;
	}

	public int getId() {
		return id;
	}

	public String getAgentId() {
		return agentId;
	}

	public String getApplicationName() {
		return applicationName;
	}

	@Override
	public String toString() {
		return "Service [agentId=" + agentId + ", applicationName=" + applicationName + ", serviceUrl=" + serviceUrl
				+ "]";
	}
	
}
