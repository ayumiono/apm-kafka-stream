package com.gb.apm.kafka.stream.apm.chain.rest.grafana.v3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gb.apm.kafka.stream.apm.chain.stream.ChainAppV3;
import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.dao.DaoException;
import com.gb.apm.kafka.stream.apm.common.dao.SpanDao;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceNode;
import com.gb.apm.kafka.stream.apm.common.model.v3.TraceSnapshot;
import com.gb.apm.kafka.stream.apm.common.rest.RestExceptionMapper.CustomRestAPIException;
import com.gb.apm.kafka.stream.apm.common.utils.TimeWindowUtils;
import com.gb.apm.model.TSpan;

/**
 * 
 * trace 快照
 * 
 * @author xuelong.chen
 *
 */
//@RestController
//@RequestMapping("/grafana/trace")
//@CrossOrigin
public class TraceRestProxy {
	
	@Autowired
	private SpanDao spanDao;
	
	@Autowired
	private ChainAppV3 app;
	
	/**
	 * trace快照
	 * @param transactionId
	 * @return
	 */
	@RequestMapping("/{transactionId}")
	List<TSpan> queryTrace(@PathVariable String transactionId) {
		try {
			//FIXME 根据parentSpanId排序
			return spanDao.traceScopeSpan(transactionId);
		} catch (DaoException e) {
			e.printStackTrace();
			return new ArrayList<TSpan>();
		}
	}
	
	/**
	 * 获取指定链路下的trace快照
	 * @param applicationId
	 * @param requestPayload
	 * @return
	 */
	List<TraceSnapshot> traceSnapshot(@PathVariable Integer applicationId, @RequestBody Map<String, Object> requestPayload) {
		try {
			Long[] timeRange = TimeWindowUtils.extractGrafanaTimeRange(requestPayload);
			Long[] fromTo = TimeWindowUtils.extractFromTo(TimeWindows.of(KafkaStreamApp.ONEHOUR).advanceBy(KafkaStreamApp.ONEHOUR), timeRange[0], timeRange[1]);
			ReadOnlyWindowStore<Integer,ServiceNode> store = app.streams.store(KafkaStreamApp.CHAIN_METRICS_STORE, QueryableStoreTypes.windowStore());
			WindowStoreIterator<ServiceNode> methodMetrics = store.fetch(applicationId, fromTo[0], fromTo[1]);
			List<ServiceNode> nodes = new ArrayList<>();
			while(methodMetrics.hasNext()) {
				nodes.add(methodMetrics.next().value);
			}
			List<String> transactionIds = new ArrayList<>();
//			for(ApplicationNode node : nodes) {
//				for(ServerInstance instance : node.getServerInstances().values()) {
//					for(TimeBucket bucket : instance.buckets(timeRange[0], timeRange[1])) {
//						transactionIds.addAll(bucket.getTransactionIds());
//					}
//				}
//			}
			return spanDao.traceSnapshot(transactionIds);
		} catch (Exception e) {
			throw new CustomRestAPIException(CustomRestAPIException.UNKNOWN_ERROR_CODE,CustomRestAPIException.UNKNOW_ERROR_MSG);
		}
		
	}
}
