package com.gb.apm.dao.mongo;

import java.util.List;

import com.gb.apm.kafka.stream.apm.common.dao.ApmSystemDao;
import com.gb.apm.kafka.stream.apm.common.model.ApmSystem;

public class ApmSystemDaoImpl extends BaseImpl<ApmSystem> implements ApmSystemDao {
	
	protected ApmSystemDaoImpl() throws Exception {
		super(ApmSystem.class);
	}

	@Override
	public List<ApmSystem> getAll() throws GBMongodbException {
		return use(APM_DB).query();
	}
}
