package com.gb.apm.kafka.stream.apm.chain.rest.grafana;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gb.apm.kafka.stream.apm.common.dao.ApmSystemDao;
import com.gb.apm.kafka.stream.apm.common.dao.ChainTreeDao;
import com.gb.apm.kafka.stream.apm.common.dao.DaoException;
import com.gb.apm.kafka.stream.apm.common.model.ApmSystem;
import com.gb.apm.kafka.stream.apm.common.model.Chain;
import com.gb.apm.kafka.stream.apm.common.model.TreeStackFrame;

//@RestController
//@RequestMapping("/grafana/metric")
//@CrossOrigin
public class MetricQueryRestProxy {
	
	@Autowired
	private ChainTreeDao chainTreeDao;
	
	@Autowired
	private ApmSystemDao apmSystemDao;
	
	@RequestMapping("/systemId")
	public List<Map<String, Object>> getSystemIdList() throws Exception{
		List<Map<String, Object>> r = new ArrayList<>();
		List<ApmSystem> systems = apmSystemDao.getAll();
		for(ApmSystem system : systems) {
			Map<String, Object> result = new HashMap<>();
			result.put("text", system.getSystem_name());
			result.put("value", system.getSystem_num_id());
			r.add(result);
		}
		return r;
	}
	
	/**
	 * 获取指定systemId下的所有chain，用于下拉列表
	 * @param systemId
	 * @return
	 */
	@RequestMapping("/chain/{systemId}")
	public List<Map<String, Object>> getChainSelectBox(@PathVariable("systemId") int systemId) throws Exception {
		List<Map<String, Object>> r = new ArrayList<>();
		Chain condition = new Chain();
		condition.setSystemId(systemId);
		List<Chain> rs = chainTreeDao.get(condition);
		for(Chain chain : rs) {
			Map<String, Object> result = new HashMap<>();
			result.put("text", chain.getEntranceMethod());
			result.put("value", chain.getId());
			r.add(result);
		}
		return r;
	}
	
	/**
	 * 获取指定chain下的所有方法
	 * @param chainId
	 * @return
	 * @throws GBMongodbException
	 */
	@RequestMapping("/chain/method/{chainId}")
	public List<Map<String, Object>> getChainMethod(@PathVariable("chainId") long chainId) throws DaoException {
		List<Map<String, Object>> result = new ArrayList<>();
		Map<String, Object> onetable = new HashMap<>();
		List<Map<String, Object>> columns = new ArrayList<>();
		columns.add(Collections.singletonMap("text", "id"));
		columns.add(Collections.singletonMap("text", "name"));
		columns.add(Collections.singletonMap("text", "type"));
		columns.add(Collections.singletonMap("text", "desc"));
		
		List<Object[]> rows = new ArrayList<>();
		Chain chain = chainTreeDao.queryTreeById(chainId);
		for(TreeStackFrame node : chain.getStacks()) {
			rows.add(new Object[] {node.getStackId(),node.getMethodName(),node.getType(),node.getMethodDesc()});
		}
		onetable.put("columns", columns);
		onetable.put("rows", rows);
		onetable.put("type", "table");
		result.add(onetable);
		return result;
	}
	
	/**
	 * 获取指定systemId下的所有chain
	 * @param systemId
	 * @return
	 */
	@RequestMapping("/chains/{systemId}")
	public List<Map<String, Object>> getChain(@PathVariable("systemId") int systemId) {
		Chain condition = new Chain();
		condition.setSystemId(systemId);
		List<Map<String, Object>> result = new ArrayList<>();
		Map<String, Object> onetable = new HashMap<>();
		List<Map<String, Object>> columns = new ArrayList<>();
		columns.add(Collections.singletonMap("text", "id"));
		columns.add(Collections.singletonMap("text", "name"));
		List<Object[]> rows = new ArrayList<>();
		try {
			List<Chain> rs = chainTreeDao.get(condition);
			for(Chain chain : rs) {
				rows.add(new Object[] {chain.getId(),chain.getEntranceMethod()});
			}
		} catch (DaoException e) {
			e.printStackTrace();
		}
		onetable.put("columns", columns);
		onetable.put("rows", rows);
		onetable.put("type", "table");
		result.add(onetable);
		return result;
	}
}
