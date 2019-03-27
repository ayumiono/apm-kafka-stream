package com.gb.apm.kafka.stream.apm.common.dao;

import java.util.List;

import com.gb.apm.kafka.stream.apm.common.model.v3.Service;

public interface ServiceDao {
	
	public boolean addServcie(Service service) throws GBPersistenceException;
	
	public List<Service> getServices() throws GBPersistenceException;
	
	public List<Service> getServices(String agentId) throws GBPersistenceException;
	
	public Service getService(int serviceId) throws GBPersistenceException;
}
