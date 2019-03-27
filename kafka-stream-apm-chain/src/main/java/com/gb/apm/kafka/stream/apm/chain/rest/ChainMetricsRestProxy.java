package com.gb.apm.kafka.stream.apm.chain.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.gb.apm.kafka.stream.apm.chain.stream.ChainApp;
import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.dao.ChainTreeDao;
import com.gb.apm.kafka.stream.apm.common.dao.DaoException;
import com.gb.apm.kafka.stream.apm.common.model.Chain;
import com.gb.apm.kafka.stream.apm.common.model.ChainMetrics;
import com.gb.apm.kafka.stream.apm.common.model.PageResult;
import com.gb.apm.kafka.stream.apm.common.rest.RestExceptionMapper.CustomRestAPIException;

//@RestController
//@RequestMapping("/chain")
public class ChainMetricsRestProxy {
	@Autowired
	private ChainApp app;
	@Autowired
	private ChainTreeDao chainTreeDao;
	
	/**
	 * 获取指定的方法统计(最近一小时)
	 * @param chain_id 链路的全局唯一标识
	 * @return
	 */
	@RequestMapping(value="/metrics/{chain_id}",method=RequestMethod.GET)
	public List<ChainMetrics> metrics(@PathVariable("chain_id") long chain_id) {
		try {
			ReadOnlyWindowStore<Long, ChainMetrics> store = 
					app.streams.store(KafkaStreamApp.CHAIN_METRICS_STORE, QueryableStoreTypes.windowStore());
			Map<Long, TimeWindow> windows =TimeWindows.of(60*60*1000).advanceBy(60*60*1000).windowsFor(System.currentTimeMillis());
			TimeWindow queryWindow = null;
			Iterator<Entry<Long, TimeWindow>> iteretor = windows.entrySet().iterator();
			while(iteretor.hasNext()) {
				queryWindow = iteretor.next().getValue();
				break;
			}
			WindowStoreIterator<ChainMetrics> methodMetrics = store.fetch(chain_id, queryWindow.start(), queryWindow.end());
			
			List<ChainMetrics> result = new ArrayList<>();
			while(methodMetrics.hasNext()) {
				result.add(methodMetrics.next().value);
			}
			return result;
		} catch (Exception e) {
			throw new CustomRestAPIException(CustomRestAPIException.UNKNOWN_ERROR_CODE,CustomRestAPIException.UNKNOW_ERROR_MSG);
		}
		
	}
	
	/**
	 * 关闭链路绘制
	 * @param host
	 * @param port
	 * @return
	 */
	@RequestMapping(value="/treepaint/shutdown",method=RequestMethod.PUT)
	public Map<String, Object> shutDownChainPaint() {
		boolean r = app.switchStatus();
		return Collections.singletonMap("status", r);
	}
	
	@RequestMapping(value="/hello",method=RequestMethod.GET)
	public boolean hello() {
		throw new CustomRestAPIException(CustomRestAPIException.MONGO_ERROR_CODE, CustomRestAPIException.MONGO_ERROR_MSG);
	}
	
	/**
	 * 链路绘制功能是否开启
	 * @param chain_id 链路的全局唯一标识
	 * @return
	 */
	@RequestMapping(value="/treepaint/status",method=RequestMethod.GET)
	public boolean isChainPaintOn() {
		return app.switcherStatus();
	}
	
	@RequestMapping(value="/chaintree/{chainId}",method=RequestMethod.GET)
	public Chain getChainTree(@PathVariable("chainId") long chainId) {
		try {
			Chain tree = chainTreeDao.queryTreeById(chainId);
			return tree;
		} catch (DaoException e) {
			throw new CustomRestAPIException(CustomRestAPIException.MONGO_ERROR_CODE,CustomRestAPIException.MONGO_ERROR_MSG);
		} catch (Exception e) {
			throw new CustomRestAPIException(CustomRestAPIException.UNKNOWN_ERROR_CODE,CustomRestAPIException.UNKNOW_ERROR_MSG);
		}
	}
	
	@RequestMapping(value="/chaintrees",method=RequestMethod.GET)
	@Deprecated
	public PageResult<Map<String,Object>> getChainTrees(@RequestParam("pageNo") int pageNo,@RequestParam("pageSize") int pageSize) {
		PageResult<Chain> pageResult = new PageResult<Chain>(pageNo,pageSize);
		try {
			pageResult = chainTreeDao.pageQuery(pageResult, null);
			List<Chain> rs = pageResult.getRs();
			List<Map<String, Object>> rs2 = new ArrayList<>();
			PageResult<Map<String,Object>> result = new PageResult<>(pageNo, pageSize);
			result.setTotalCount(pageResult.getTotalCount());
			for(Chain chain : rs) {
				Map<String, Object> m = new HashMap<>();
				m.put("chainId", chain.getId());
				m.put("systemId", chain.getSystemId());
				m.put("nodeCount", chain.getStacks().size());
				m.put("createTime", chain.getCreateTime());
				m.put("updateTime", chain.getUpdateTime());
				m.put("systemName", chain.getSystemName());
				m.put("entranceMethod", chain.getEntranceMethod());
				rs2.add(m);
			}
			result.setRs(rs2);
			return result;
		} catch (DaoException e) {
			throw new CustomRestAPIException(CustomRestAPIException.MONGO_ERROR_CODE, CustomRestAPIException.MONGO_ERROR_MSG);
		} catch (Exception e) {
			throw new CustomRestAPIException(CustomRestAPIException.UNKNOWN_ERROR_CODE,CustomRestAPIException.UNKNOW_ERROR_MSG);
		}
	}
}
