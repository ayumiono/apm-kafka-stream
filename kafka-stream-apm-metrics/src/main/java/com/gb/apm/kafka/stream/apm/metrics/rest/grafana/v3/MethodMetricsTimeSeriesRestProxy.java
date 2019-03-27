package com.gb.apm.kafka.stream.apm.metrics.rest.grafana.v3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.dao.SpanDao;
import com.gb.apm.kafka.stream.apm.common.grafana.TimeSeriesData;
import com.gb.apm.kafka.stream.apm.common.model.TimeBucket;
import com.gb.apm.kafka.stream.apm.common.model.v3.CodeMetricsV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.MethodMetrics;
import com.gb.apm.kafka.stream.apm.common.model.v3.TraceSnapshot;
import com.gb.apm.kafka.stream.apm.common.rest.RestExceptionMapper.CustomRestAPIException;
import com.gb.apm.kafka.stream.apm.common.utils.TimeWindowUtils;

@RestController
@RequestMapping("/grafana/method")
@CrossOrigin
public class MethodMetricsTimeSeriesRestProxy {

	private KafkaStreams streams;
	
	@Autowired
	private SpanDao spanDao;

	public MethodMetricsTimeSeriesRestProxy(KafkaStreamApp app) {
		this.streams = app.streams();
	}

	/**
	 * 获取指定客户端下的指定错误码关联的trace快照
	 * @param agentId
	 * @param code
	 * @return
	 */
	@RequestMapping(value = "/trace/code/{agentId}/{code}", method = RequestMethod.POST)
	public List<TraceSnapshot> agentCodeScopeTrace(@PathVariable int agentId, @PathVariable long code, @RequestBody Map<String,Object> requestPayload) {
		try {
			Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
			Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(60*60*1000).advanceBy(60*60*1000), timeRange[0], timeRange[1]);
			ReadOnlyWindowStore<Integer, CodeMetricsV2> store = 
					streams.store(KafkaStreamApp.CODE_METRICS_STORE, QueryableStoreTypes.<Integer, CodeMetricsV2>windowStore());
			WindowStoreIterator<CodeMetricsV2> methodMetrics = store.fetch(agentId, fromTo[0], fromTo[1]);
			List<String> transactionIds = new ArrayList<>();
			List<CodeMetricsV2> codeMetricses = new ArrayList<>();
			while(methodMetrics.hasNext()) {
				codeMetricses.add(methodMetrics.next().value);
			}
			for(CodeMetricsV2 codeMetrics : codeMetricses) {
				List<String> tids = codeMetrics.getExceptionTransactionids().get(code);
				transactionIds.addAll(tids);
			}
			return spanDao.traceSnapshot(transactionIds);
		} catch (Exception e) {
			throw new CustomRestAPIException(CustomRestAPIException.UNKNOWN_ERROR_CODE,CustomRestAPIException.UNKNOW_ERROR_MSG);
		}
	}
	
	/**
	 * 获取指定方法节点的trace快照（对于任何方法都是通用的）,sql是特例
	 * @param methodId
	 * @param requestPayload
	 * @return
	 */
	@RequestMapping(value = "/trace/sql/{methodId}", method = RequestMethod.POST)
	public List<TraceSnapshot> sqlScopeTrace(@PathVariable int methodId, @RequestBody Map<String,Object> requestPayload) {
		try {
			Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
			Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(60*60*1000).advanceBy(60*60*1000), timeRange[0], timeRange[1]);
			ReadOnlyWindowStore<Integer, MethodMetrics> store = 
					streams.store(KafkaStreamApp.METHOD_INVOKE_METRICS_METHODLEVEL_STORE, QueryableStoreTypes.<Integer, MethodMetrics>windowStore());
			WindowStoreIterator<MethodMetrics> iterator = store.fetch(methodId, fromTo[0], fromTo[1]);
			List<String> transactionIds = new ArrayList<>();
			List<MethodMetrics> methodMetricses = new ArrayList<>();
			while(iterator.hasNext()) {
				methodMetricses.add(iterator.next().value);
			}
			for(MethodMetrics methodMetrics : methodMetricses) {
				List<TimeBucket> buckets = methodMetrics.buckets(timeRange[0], timeRange[1]);
				for(TimeBucket bucket : buckets) {
					//FIXME
				}
			}
			return spanDao.traceSnapshot(transactionIds);
		} catch (Exception e) {
			throw new CustomRestAPIException(CustomRestAPIException.UNKNOWN_ERROR_CODE,CustomRestAPIException.UNKNOW_ERROR_MSG);
		}
	}
	
	/**
	 * 实时统计数据
	 * @param stackId
	 * @param requestPayload
	 * @return
	 */
	@RequestMapping(value="/timebucket",method=RequestMethod.POST)
	public List<TimeSeriesData> grafanaChainLevelTimeBucket(@RequestParam("stackId") long stackId,@RequestBody Map<String, Object> requestPayload) {
		try {
			Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
			Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(60*60*1000).advanceBy(60*60*1000), timeRange[0], timeRange[1]);
			
			ReadOnlyWindowStore<Long, MethodMetrics> store = 
					streams.store(KafkaStreamApp.METHOD_INVOKE_METRICS_STORE, QueryableStoreTypes.windowStore());
			
			WindowStoreIterator<MethodMetrics> methodMetrics = store.fetch(stackId, fromTo[0], fromTo[1]);
			
			List<TimeBucket> result = new ArrayList<>();
			while(methodMetrics.hasNext()) {
				result.addAll(methodMetrics.next().value.buckets(timeRange[0],timeRange[1]));
			}
			return TimeSeriesData.wrapTimebucketData(result);
		} catch (Exception e) {
			throw new CustomRestAPIException(CustomRestAPIException.UNKNOWN_ERROR_CODE,CustomRestAPIException.UNKNOW_ERROR_MSG);
		}
	}
	
	/**
	 * 获取统计数据
	 * @param stackId
	 * @param requestPayload
	 * @return
	 */
	public Map<String, Integer> statistics(@RequestParam("stackId") int stackId,@RequestBody Map<String, Object> requestPayload) {
		try {
			Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
			Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(60*60*1000).advanceBy(60*60*1000), timeRange[0], timeRange[1]);
			
			ReadOnlyWindowStore<Integer, MethodMetrics> store = 
					streams.store(KafkaStreamApp.METHOD_INVOKE_METRICS_STORE, QueryableStoreTypes.windowStore());
			
			WindowStoreIterator<MethodMetrics> methodMetrics = store.fetch(stackId, fromTo[0], fromTo[1]);
			
			List<MethodMetrics> result = new ArrayList<>();
			while(methodMetrics.hasNext()) {
				result.add(methodMetrics.next().value);
			}
			int invokeCount=0,errorCount=0,exceptionCount=0,averageSpent =0,maxSpent =Integer.MIN_VALUE,minSpent =Integer.MAX_VALUE;
			for(MethodMetrics metrics : result) {
				invokeCount += metrics.getInvokeCount();
				averageSpent += metrics.getAverageSpent();
				errorCount += metrics.getErrorCount();
				exceptionCount += metrics.getExceptionCount();
				if(maxSpent < metrics.getMaxSpent()) {
					maxSpent = (int) metrics.getMaxSpent();
				}
				if(minSpent > metrics.getMinSpent()) {
					minSpent = (int) metrics.getMinSpent();
				}
			}
			averageSpent = averageSpent/result.size();
			Map<String, Integer> statistics = new HashMap<>();
			statistics.put("invokeCount", invokeCount);
			statistics.put("averageSpent", averageSpent);
			statistics.put("exceptionCount", exceptionCount);
			statistics.put("errorCount", errorCount);
			statistics.put("maxSpent", maxSpent);
			statistics.put("minSpent", minSpent);
			return statistics;
		} catch (Exception e) {
			throw new CustomRestAPIException(CustomRestAPIException.UNKNOWN_ERROR_CODE,CustomRestAPIException.UNKNOW_ERROR_MSG);
		}
	}
}
