package com.gb.apm.dao.mongo;

import java.util.List;

import com.gb.apm.kafka.stream.apm.common.dao.ChainTreeDaoV2;
import com.gb.apm.kafka.stream.apm.common.model.PageResult;
import com.gb.apm.kafka.stream.apm.common.model.v3.Chain;

public class ChainTreeDaoImplV3 extends BaseImpl<Chain> implements ChainTreeDaoV2 {

	public ChainTreeDaoImplV3() throws Exception {
		super(Chain.class);
	}

	@Override
	public String insertTree(Chain chain) throws GBMongodbException {
		String _id = use(APM_DB).insert(chain);
		return _id;
	}

	@Override
	public List<Chain> queryTreeBySystemId(int systemId) throws GBMongodbException {
		Chain condition = new Chain();
		return use(APM_DB).query(condition);//TODO
	}
	
	@Override
	public boolean exist(long id) throws GBMongodbException {
		Chain condition = new Chain();
		List<Chain> r = use(APM_DB).query(condition);
		if(r.size() > 0) {
			return true;
		}
		return false;
	}

	@Override
	public Chain queryTreeById(long id) throws GBMongodbException {
		//TODO
		Chain condition = new Chain();
		return condition;
	}

	@Override
	public List<Chain> getAll() throws GBMongodbException {
		return use(APM_DB).query();
	}

	@Override
	public PageResult<Chain> pageQuery(PageResult<Chain> p, Chain chain) throws GBMongodbException {
		use(APM_DB).page(p, chain);
		return p;
	}

	@Override
	public void updateTree(Chain chain) throws GBMongodbException {
		use(APM_DB).updateById(chain);
	}

	@Override
	public List<Chain> get(Chain chain) throws GBMongodbException {
		List<Chain> r = use(APM_DB).query(chain);
		return r;
	}
}
