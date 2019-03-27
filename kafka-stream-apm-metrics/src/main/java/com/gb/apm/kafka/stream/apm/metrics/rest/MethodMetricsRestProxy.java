package com.gb.apm.kafka.stream.apm.metrics.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.dao.ChainTreeDao;
import com.gb.apm.kafka.stream.apm.common.dao.DaoException;
import com.gb.apm.kafka.stream.apm.common.model.Chain;
import com.gb.apm.kafka.stream.apm.common.model.MethodMetrics;
import com.gb.apm.kafka.stream.apm.common.model.MethodMetricsBrief;
import com.gb.apm.kafka.stream.apm.common.model.PageResult;
import com.gb.apm.kafka.stream.apm.common.model.TreeStackFrame;

//@RestController
//@RequestMapping("/method")
public class MethodMetricsRestProxy {
	
	private KafkaStreams streams;
	//添加到实时监控中的方法，从数据库中获取并缓存
	private ConcurrentHashSet<String> monitors = new ConcurrentHashSet<>();
	
	@Autowired
	private ChainTreeDao chainTreeDao;

	public MethodMetricsRestProxy(KafkaStreamApp app) {
		this.streams = app.streams();
	}
	
	/**
	 * 获取指定的方法统计(最近一小时)(方法级别)
	 * @param chain_id 链路ID
	 * @param method_name 方法名
	 * @return
	 */
	@RequestMapping(value="/metrics/methodlevel/{methodId}",method = RequestMethod.GET)
	public List<MethodMetrics> getMetricsMethodLevel(@PathVariable("methodId") long methodId){
		ReadOnlyWindowStore<Long, MethodMetrics> store = 
				streams.store(KafkaStreamApp.METHOD_INVOKE_METRICS_METHODLEVEL_STORE, QueryableStoreTypes.windowStore());
		Map<Long, TimeWindow> windows =TimeWindows.of(60*60*1000).advanceBy(60*60*1000).windowsFor(System.currentTimeMillis());
		TimeWindow queryWindow = null;
		Iterator<Entry<Long, TimeWindow>> iteretor = windows.entrySet().iterator();
		while(iteretor.hasNext()) {
			queryWindow = iteretor.next().getValue();
			break;
		}
		
		WindowStoreIterator<MethodMetrics> methodMetrics = store.fetch(methodId, queryWindow.start(), queryWindow.end());
		
		List<MethodMetrics> result = new ArrayList<>();
		while(methodMetrics.hasNext()) {
			MethodMetrics v = methodMetrics.next().value;
			v.setStartMs(queryWindow.start());
			v.setEndMs(queryWindow.end());
			result.add(v);
		}
		return result;
	}
	
	@RequestMapping(value="/metrics/underchain/{chainId}",method = RequestMethod.GET)
	public PageResult<MethodMetricsBrief> getAllMethodMetricsUnderChainId(@PathVariable("chainId") long chainId,@RequestParam("pageSize") int pageSize,@RequestParam("pageNo") int pageNo){
		
		PageResult<MethodMetricsBrief> result = new PageResult<>(pageNo, pageSize);
		try {
			Chain chain = chainTreeDao.queryTreeById(chainId);
			List<TreeStackFrame> stacks = chain.getStacks();
			ReadOnlyWindowStore<Long, MethodMetrics> store = 
					streams.store(KafkaStreamApp.METHOD_INVOKE_METRICS_STORE, QueryableStoreTypes.windowStore());
			Map<Long, TimeWindow> windows =TimeWindows.of(60*60*1000).advanceBy(60*60*1000).windowsFor(System.currentTimeMillis());
			TimeWindow queryWindow = null;
			Iterator<Entry<Long, TimeWindow>> iteretor = windows.entrySet().iterator();
			while(iteretor.hasNext()) {
				queryWindow = iteretor.next().getValue();
				break;
			}
			List<MethodMetricsBrief> innerRs = new ArrayList<>();
			for(int i = (pageNo-1)*pageSize;i< Math.min(stacks.size(), pageNo*pageSize);i++) {
				WindowStoreIterator<MethodMetrics> methodMetrics = store.fetch(stacks.get(i).getStackId(), queryWindow.start(), queryWindow.end());
				while(methodMetrics.hasNext()) {
					MethodMetrics v = methodMetrics.next().value;
					MethodMetricsBrief brief = MethodMetricsBrief.brief(v);
					brief.setMethodId(stacks.get(i).getStackId());
					innerRs.add(brief);
					break;
				}
			}
			result.setRs(innerRs);
			result.setTotalCount(stacks.size());
			return result;
		} catch (DaoException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 获取指定的方法统计(最近一小时)(链路级别)
	 * @param chain_id 链路ID
	 * @param method_name 方法名
	 * @return
	 */
	@RequestMapping(value="/metrics/chainlevel/{methodId}",method = RequestMethod.GET)
	public List<MethodMetrics> getMetricsChainLevel(@PathVariable("methodId") long methodId) {
		ReadOnlyWindowStore<Long, MethodMetrics> store = 
				streams.store(KafkaStreamApp.METHOD_INVOKE_METRICS_STORE, QueryableStoreTypes.windowStore());
		Map<Long, TimeWindow> windows =TimeWindows.of(60*60*1000).advanceBy(60*60*1000).windowsFor(System.currentTimeMillis());
		TimeWindow queryWindow = null;
		Iterator<Entry<Long, TimeWindow>> iteretor = windows.entrySet().iterator();
		while(iteretor.hasNext()) {
			queryWindow = iteretor.next().getValue();
			break;
		}
		
		WindowStoreIterator<MethodMetrics> methodMetrics = store.fetch(methodId, queryWindow.start(), queryWindow.end());
		
		List<MethodMetrics> result = new ArrayList<>();
		while(methodMetrics.hasNext()) {
			MethodMetrics v = methodMetrics.next().value;
			result.add(v);
		}
		return result;
	}
	
	/**
	 * 获取调用次数前top的方法统计
	 * @param top
	 * @return
	 */
	@RequestMapping(value="/top/{top}",method = RequestMethod.GET)
	public List<MethodMetrics> top(@PathVariable("top") int top) {
		// TODO
		return null;
	}

	/**
	 * 测试
	 * @return
	 */
	@RequestMapping(value="/hello",method = RequestMethod.GET)
	public MethodMetrics hello() {
		return new MethodMetrics();
	}
	
	/**
	 * 提交监控请求
	 * @param methodDesc
	 * @return
	 */
	@RequestMapping(value="/submit/{method_id}",method = RequestMethod.POST)
	public Map<String, Object> submitMonitor(@RequestBody String method_id){
		if(method_id == null) {
			return Collections.singletonMap("msg", "params can not be empty");
		}else if(monitors.contains(method_id)){
			return Collections.singletonMap("msg", "monitor already exist");
		} else {
			monitors.add(method_id);
			return Collections.singletonMap("msg", "add to monitor sets success.");
		}
		
	}
}
