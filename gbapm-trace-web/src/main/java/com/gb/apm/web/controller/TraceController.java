package com.gb.apm.web.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gb.apm.common.trace.AnnotationKey;
import com.gb.apm.common.trace.ServiceType;
import com.gb.apm.kafka.stream.apm.common.model.utils.JacksonUtils;
import com.gb.apm.model.TSpan;
import com.gb.apm.plugins.commbiz.CommBizConstants;
import com.gb.apm.plugins.dubbo.DubboConstants;
import com.gb.apm.plugins.mysql.MySqlConstants;
import com.gb.apm.web.controller.QueryTraceIdModel.Condition;
import com.gb.apm.web.timer.RolloverIndicesTimer;

@RestController
@CrossOrigin
public class TraceController {
	
	private static final Logger log = LoggerFactory.getLogger(TraceController.class);
	
	@Autowired
	TransportClient client;

	@RequestMapping("/trace/{transactionId}")
	public List<TSpan> trace(@PathVariable("transactionId") String transactionId) {
		List<TSpan> result = new ArrayList<TSpan>();
		try {
			TermQueryBuilder tqb = QueryBuilders.termQuery("transactionId", transactionId);
			SearchRequestBuilder searchBuilder = this.client.prepareSearch(RolloverIndicesTimer.ALIAS_SEARCH_GB_APM_TRACE)
					.setTypes("trace")
					.setVersion(true)
					.setSearchType(SearchType.DEFAULT)
					.setQuery(tqb)
					.setFrom(0)
					.setSize(1000);
			SearchResponse response = searchBuilder.execute().get();
			long totalDocs = response.getHits().totalHits;
			log.info("total tspan:{} under traceId:{}", totalDocs,transactionId);
			for (SearchHit hit : response.getHits()) {
				if (hit != null) {
					result.add(JacksonUtils.toObject(hit.getSourceAsString(), TSpan.class));
				}
			}
			return result;
		} catch (Exception e) {
			log.error(String.format("查询%s失败", RolloverIndicesTimer.ALIAS_SEARCH_GB_APM_TRACE), e);
		}
		return result;
	}
	
	@RequestMapping("/traceId/query")
	public Map<String,Object> trace(@RequestBody QueryTraceIdModel queryModel) {
		try {
			if(queryModel.getTraceId() != null) {
				return queryTrace(Collections.singletonList(queryModel.getTraceId()),queryModel.getPageIndex(),queryModel.getPageSize());
			}else {
				BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
				Date[] time_range = queryModel.getTime_range();
				int agentId = queryModel.getAgentId();
				List<Condition> conditions = queryModel.getConditions();
				boolQueryBuilder.must(QueryBuilders.termQuery("agentId", agentId)).must(QueryBuilders.rangeQuery("startTime").gt(time_range[0].getTime()).lt(time_range[1].getTime()));
				for(Condition condition : conditions) {
					if(condition.getServiceType() == DubboConstants.DUBBO_PROVIDER_SERVICE_TYPE.getCode()) {
						boolQueryBuilder.must(QueryBuilders.termQuery("serviceType", DubboConstants.DUBBO_PROVIDER_SERVICE_TYPE.getCode()));
						if(condition.getApi() != null) {
							boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("rpc", condition.getApi()));
						}
						if(condition.getArgs() != null) {
							boolQueryBuilder.must(QueryBuilders.nestedQuery("annotations", 
									QueryBuilders.boolQuery()
									.must(QueryBuilders.termQuery("annotations.key", DubboConstants.DUBBO_ARGS_ANNOTATION_KEY.getCode()))
									.must(QueryBuilders.matchPhraseQuery("annotations.value", condition.getArgs()))
									, ScoreMode.None));
						}
						if(condition.getResult() != null) {
							boolQueryBuilder.must(QueryBuilders.nestedQuery("annotations", 
									QueryBuilders.boolQuery()
									.must(QueryBuilders.termQuery("annotations.key", DubboConstants.DUBBO_RESULT_ANNOTATION_KEY.getCode()))
									.must(QueryBuilders.matchPhraseQuery("annotations.value", condition.getResult())),
							ScoreMode.None));
						}
					}else if(condition.getServiceType() == DubboConstants.DUBBO_CONSUMER_SERVICE_TYPE.getCode()) {
						boolQueryBuilder.must(QueryBuilders.nestedQuery("tspanEventList", QueryBuilders.termQuery("tspanEventList.serviceType", DubboConstants.DUBBO_CONSUMER_SERVICE_TYPE.getCode()), ScoreMode.None));
						if(condition.getApi() != null) {
							boolQueryBuilder.must(QueryBuilders.nestedQuery("tspanEventList.annotations",
									QueryBuilders.boolQuery()
									.must(QueryBuilders.termQuery("tspanEventList.annotations.key", DubboConstants.DUBBO_INTERFACE_ANNOTATION_KEY.getCode()))
									.must(QueryBuilders.matchPhraseQuery("tspanEventList.annotations.value", condition.getApi())),
							ScoreMode.None));
						}
						if(condition.getArgs() != null) {
							boolQueryBuilder.must(QueryBuilders.nestedQuery("tspanEventList.annotations", 
									QueryBuilders.boolQuery()
									.must(QueryBuilders.termQuery("tspanEventList.annotations.key", DubboConstants.DUBBO_ARGS_ANNOTATION_KEY.getCode()))
									.must(QueryBuilders.matchPhraseQuery("tspanEventList.annotations.value", condition.getArgs())),
							ScoreMode.None));
						}
						if(condition.getResult() != null) {
							boolQueryBuilder.must(QueryBuilders.nestedQuery("tspanEventList.annotations", 
									QueryBuilders.boolQuery()
									.must(QueryBuilders.termQuery("tspanEventList.annotations.key", DubboConstants.DUBBO_RESULT_ANNOTATION_KEY.getCode()))
									.must(QueryBuilders.matchPhraseQuery("tspanEventList.annotations.value", condition.getResult())),
							ScoreMode.None));
						}
					}else if(condition.getServiceType() == ServiceType.INTERNAL_METHOD.getCode()) {
						boolQueryBuilder.must(QueryBuilders.nestedQuery("tspanEventList", QueryBuilders.termQuery("tspanEventList.serviceType", ServiceType.INTERNAL_METHOD.getCode()), ScoreMode.None));
						if(condition.getApi() != null) {
							boolQueryBuilder.must(QueryBuilders.nestedQuery("tspanEventList.annotations",
									QueryBuilders.boolQuery()
									.must(QueryBuilders.termQuery("tspanEventList.annotations.key", AnnotationKey.API.getCode()))
									.must(QueryBuilders.matchPhraseQuery("tspanEventList.annotations.value", condition.getApi())), 
							ScoreMode.None));
						}
						
						if(condition.getArgs() != null) {
							boolQueryBuilder.must(QueryBuilders.nestedQuery("tspanEventList.annotations",
									QueryBuilders.boolQuery()
									.must(QueryBuilders.termQuery("tspanEventList.annotations.key", CommBizConstants.COMMBIZ_API_ARGS.getCode()))
									.must(QueryBuilders.matchPhraseQuery("tspanEventList.annotations.value", condition.getArgs())),
							ScoreMode.None));
						}
						if(condition.getResult() != null) {
							boolQueryBuilder.must(QueryBuilders.nestedQuery("tspanEventList.annotations",
									QueryBuilders.boolQuery()
									.must(QueryBuilders.termQuery("tspanEventList.annotations.key", AnnotationKey.RETURN_DATA.getCode()))
									.must(QueryBuilders.matchPhraseQuery("tspanEventList.annotations.value", condition.getResult())),
							ScoreMode.None));
						}
					}else if(condition.getServiceType() == MySqlConstants.MYSQL_EXECUTE_QUERY.getCode()) {
						boolQueryBuilder.must(QueryBuilders.nestedQuery("tspanEventList", QueryBuilders.termQuery("tspanEventList.serviceType", MySqlConstants.MYSQL_EXECUTE_QUERY.getCode()), ScoreMode.None));
						if(condition.getApi() != null) {
							boolQueryBuilder.must(QueryBuilders.nestedQuery("tspanEventList.annotations",
									QueryBuilders.boolQuery()
									.must(QueryBuilders.termQuery("tspanEventList.annotations.key", AnnotationKey.SQL.getCode()))
									.must(QueryBuilders.matchPhraseQuery("tspanEventList.annotations.value", condition.getApi())), 
							ScoreMode.None));
						}
						if(condition.getArgs() != null) {
							boolQueryBuilder.must(QueryBuilders.nestedQuery("tspanEventList.annotations",
									QueryBuilders.boolQuery()
									.must(QueryBuilders.termQuery("tspanEventList.annotations.key", AnnotationKey.SQL.getCode()))
									.must(QueryBuilders.matchPhraseQuery("tspanEventList.annotations.value", condition.getArgs())),
							ScoreMode.None));
						}
					}
				}
				
				Set<String> transactionIds = new HashSet<>();
				SearchRequestBuilder searchRequest = this.client.prepareSearch(RolloverIndicesTimer.ALIAS_SEARCH_GB_APM_TRACE)
						.setTypes("trace")
						.setQuery(boolQueryBuilder)
						.setFrom(0)
						.addSort("startTime", SortOrder.DESC)
						.setSize(1000);
				SearchResponse response = searchRequest.execute().get();
				for (SearchHit hit : response.getHits()) {
					String transactionId = (String) hit.getSourceAsMap().get("transactionId");
					transactionIds.add(transactionId);
	            }
				return queryTrace(transactionIds,queryModel.getPageIndex(),queryModel.getPageSize());
			}
		} catch (Exception e) {
			log.error("查询es失败",e);
		}
		return null;
	}
	
	private Map<String,Object> queryTrace(Collection<String> traceId, int pageIndex, int pageSize) throws InterruptedException, ExecutionException {
		Map<String,Object> r = new HashMap<>();
		List<Map<String, Object>> result = new ArrayList<>();
		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
		boolQueryBuilder.must(QueryBuilders.termsQuery("transactionId", traceId)).mustNot(QueryBuilders.existsQuery("parentSpanId"));
		SearchRequestBuilder searchRequest = this.client.prepareSearch()
				.setIndices(RolloverIndicesTimer.ALIAS_SEARCH_GB_APM_TRACE)
				.setTypes("trace")
				.setQuery(boolQueryBuilder)
				.setFrom(pageIndex*pageSize)
				.setSize(pageSize);
		SearchResponse response = searchRequest.execute().get();
		r.put("total", response.getHits().totalHits);
		for (SearchHit hit : response.getHits()) {
			if (hit != null) {
				String transactionId = (String) hit.getSourceAsMap().get("transactionId");
				Long startTime = (Long) hit.getSourceAsMap().get("startTime");
				Map<String, Object> map = new HashMap<>();
				map.put("transactionId", transactionId);
				map.put("startTime", startTime);
				result.add(map);
			}
		}
		r.put("result", result);
		return r;
	}
}
