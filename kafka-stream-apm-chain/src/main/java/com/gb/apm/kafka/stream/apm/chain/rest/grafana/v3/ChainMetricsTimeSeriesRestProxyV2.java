package com.gb.apm.kafka.stream.apm.chain.rest.grafana.v3;

import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gb.apm.plugins.dubbo.DubboConstants;
import com.gb.apm.plugins.mysql.MySqlConstants;
import com.gb.apm.kafka.stream.apm.common.dao.ServiceDao;
import com.gb.apm.kafka.stream.apm.common.dao.ServiceLinkDao;
import com.gb.apm.kafka.stream.apm.common.dao.ChainTreeDaoV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceLinkMetaData;
import com.gb.apm.kafka.stream.apm.common.model.v3.Chain;
import com.gb.apm.kafka.stream.apm.common.model.v3.TreeStackFrame;
import com.gb.apm.kafka.stream.apm.common.rest.RestExceptionMapper.CustomRestAPIException;


//@RestController
//@RequestMapping("/grafana/chain")
//@CrossOrigin
public class ChainMetricsTimeSeriesRestProxyV2 {
	
	@Autowired
	ServiceDao serviceDao;
	
	@Autowired
	ServiceLinkDao serviceLinkDao;
	
	@Autowired
	ChainTreeDaoV2 chainTreeDao;
	
	/**
	 * 获取应用拓扑结构
	 * @param applicationId
	 * @return
	 */
	@RequestMapping(value="/topology/{applicationId}")
	public ServiceLinkMetaData topology(@PathVariable("applicationId") int applicationId) {
		try {
			return serviceLinkDao.findServiceLink(applicationId);
		} catch (Exception e) {
			throw new CustomRestAPIException(CustomRestAPIException.UNKNOWN_ERROR_CODE,CustomRestAPIException.UNKNOW_ERROR_MSG);
		}
	}
	
	/**
	 * 查询chain链路详情
	 * @param applicationId
	 * @param rpcOnly	只显示rpc节点
	 * @param sqlOnly	只显示sql节点
	 * @return
	 */
	@RequestMapping(value="/tree/{applicationId}")
	public Chain tree(@PathVariable("applicationId") int applicationId, @RequestParam("rpcOnly") Boolean rpcOnly, @RequestParam("sqlOnly") Boolean sqlOnly) {
		
		try {
			Chain chain = chainTreeDao.queryTreeById(applicationId);
			List<TreeStackFrame> stacks = chain.getStacks();
			Iterator<TreeStackFrame> iterator = stacks.iterator();
			while(iterator.hasNext()) {
				TreeStackFrame stack = iterator.next();
				int serviceType = stack.getServiceType();
				if(rpcOnly && serviceType != DubboConstants.DUBBO_CONSUMER_SERVICE_TYPE.getCode()) {
					iterator.remove();
				}
				if(sqlOnly && serviceType != MySqlConstants.MYSQL_EXECUTE_QUERY.getCode()) {
					iterator.remove();
				}
			}
			return chain;
		} catch (Exception e) {
			throw new CustomRestAPIException(CustomRestAPIException.UNKNOWN_ERROR_CODE,CustomRestAPIException.UNKNOW_ERROR_MSG);
		}
	}
}
