package com.gb.apm.dao.mongo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gb.apm.kafka.stream.apm.common.dao.ChainTreeDao;
import com.gb.apm.kafka.stream.apm.common.model.Chain;
import com.gb.apm.kafka.stream.apm.common.model.PageResult;
import com.gb.apm.kafka.stream.apm.common.model.TreeStackFrame;

public class ChainTreeDaoImpl extends BaseImpl<Chain> implements ChainTreeDao {

	public ChainTreeDaoImpl() throws Exception {
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
		condition.setSystemId(systemId);
		condition.setType(0);//默认不展示rpc链路
		return use(APM_DB).query(condition);
	}
	
	@Override
	public boolean exist(long id) throws GBMongodbException {
		Chain condition = new Chain();
		condition.setId(id);
		List<Chain> r = use(APM_DB).query(condition);
		if(r.size() > 0) {
			return true;
		}
		return false;
	}

	@Override
	public Chain queryTreeById(long id) throws GBMongodbException {
		Chain condition = new Chain();
		condition.setId(id);
		List<Chain> r = use(APM_DB).query(condition);
		if (r.size() == 0) {
			return null;
		} else if (r.size() > 1) {
			throw new GBMongodbException(String.format("babysitter.apm_chain_tree 根据id{%s} 查到不止一条数据", id));
		} else {
			Chain chain = r.get(0);
			Iterator<TreeStackFrame> iterator = chain.getStacks().iterator();
			List<TreeStackFrame> replacement = new ArrayList<>();
			while(iterator.hasNext()) {
				TreeStackFrame frame = iterator.next();
				if(frame.getType() == 1) {//如果当前方法桢是RPC调用，则查找到对应的rpc方法链，替换掉当前的frame
					String methodDesc = frame.getMethodDesc();
					//条件type=1 rpcStubMethodDesc=methodDesc的链路
					Chain rpcChainCondition = new Chain();
					rpcChainCondition.setType(1);
					rpcChainCondition.setRpcStubMethodDesc(methodDesc);
					List<Chain> rpcChains = use(APM_DB).query(rpcChainCondition);
					if(rpcChains.size() > 1) {
						throw new GBMongodbException(String.format("babysitter.apm_chain_tree 根据type:1 rpcStubMethodDesc:{%s} 查到不止一条数据", methodDesc));
					} else if(rpcChains.size() == 0) {
						continue;
					}else {
						Chain rpcChain = rpcChains.get(0);
						iterator.remove();
						replacement.addAll(rpcChain.getStacks());
					}
				}
			}
			if(replacement.size() > 0) {
				chain.getStacks().addAll(replacement);
			}
			return chain;
		}
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
