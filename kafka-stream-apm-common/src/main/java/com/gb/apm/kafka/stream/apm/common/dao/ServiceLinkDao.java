package com.gb.apm.kafka.stream.apm.common.dao;

import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceLink;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceLinkMetaData;

public interface ServiceLinkDao {
	public void addServiceLink(ServiceLink link) throws GBPersistenceException;
	public ServiceLinkMetaData findServiceLink(int key) throws GBPersistenceException;
	public ServiceLink findServiceLinks(int serviceId);
}
