package com.gb.apm.dao.mongo;

import java.util.List;

import com.gb.apm.kafka.stream.apm.common.dao.GBPersistenceException;
import com.gb.apm.kafka.stream.apm.common.dao.ServiceDao;
import com.gb.apm.kafka.stream.apm.common.model.v3.Service;

public class ServiceDaoImpl extends BaseImpl<Service> implements ServiceDao {

	protected ServiceDaoImpl() throws Exception {
		super(Service.class);
	}

	@Override
	public boolean addServcie(Service service) throws GBPersistenceException {
		try {
			use(APM_DB).insert(service);
			return true;
		} catch (GBMongodbException e) {
			throw new GBPersistenceException("添加service失败", e);
		}
	}

	@Override
	public List<Service> getServices() throws GBPersistenceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Service> getServices(String agentId) throws GBPersistenceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Service getService(int serviceId) throws GBPersistenceException {
		// TODO Auto-generated method stub
		return null;
	}

}
