package com.gb.apm.dao.es;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQueryBuilder;
import org.springframework.stereotype.Repository;

import com.gb.apm.kafka.stream.apm.common.dao.GBPersistenceException;
import com.gb.apm.kafka.stream.apm.common.dao.ServiceDao;
import com.gb.apm.kafka.stream.apm.common.model.utils.JacksonUtils;
import com.gb.apm.kafka.stream.apm.common.model.v3.Service;

@Repository
public class ServiceDaoImpl implements ServiceDao {

	private static final Logger log = LoggerFactory.getLogger(ServiceDaoImpl.class);
	
	@Autowired
	private ElasticsearchTemplate esTemplate;

	@Override
	public boolean addServcie(Service service) throws GBPersistenceException {
		log.info("添加service节点到es");
		try {
			UpdateQueryBuilder upsert = new UpdateQueryBuilder();
			IndexRequest indexRequest = new IndexRequest("gb_apm_service", "service", service.getId() + "").source(JacksonUtils.toJson(service),XContentType.JSON);
			upsert.withId(service.getId() + "").withIndexName("gb_apm_service").withType("service").withDoUpsert(true)
				.withIndexRequest(indexRequest);
			esTemplate.update(upsert.build());
			log.info("添加service节点到es完成");
			return true;
		} catch (Exception e) {
			throw new GBPersistenceException("添加service失败", e);
		}

	}

	@Override
	public List<Service> getServices() throws GBPersistenceException {
		try {
			List<Service> result = 
			esTemplate.query(
					new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery())
							.withIndices("gb_apm_service").withTypes("service").withPageable(PageRequest.of(0, 10000)).build(),
					new ResultsExtractor<List<Service>>() {
						@Override
						public List<Service> extract(SearchResponse response) {
							List<Service> result = new ArrayList<>();
							for (SearchHit hit : response.getHits()) {
								try {
									result.add(JacksonUtils.toObject(hit.getSourceAsString(), Service.class));
								} catch (Exception e) {
									e.printStackTrace();
									return null;
								}
							}
							return result;
						}
					});
			log.info("共查询到{}条service记录",result.size());
			return result;
		} catch (Exception e) {
			throw new GBPersistenceException("查询service失败", e);
		}
	}

	@Override
	public List<Service> getServices(String agentId) throws GBPersistenceException {
		try {
			return esTemplate.query(
					new NativeSearchQueryBuilder().withQuery(QueryBuilders.termQuery("agentId", agentId))
							.withIndices("gb_apm_service").withTypes("service").withPageable(PageRequest.of(0, 10000)).build(),
					new ResultsExtractor<List<Service>>() {
						@Override
						public List<Service> extract(SearchResponse response) {
							List<Service> result = new ArrayList<>();
							for (SearchHit hit : response.getHits()) {
								try {
									result.add(JacksonUtils.toObject(hit.getSourceAsString(), Service.class));
								} catch (Exception e) {
									e.printStackTrace();
									return null;
								}
							}
							return result;
						}
					});
		} catch (Exception e) {
			throw new GBPersistenceException("查询service失败", e);
		}
	}

	@Override
	public Service getService(int serviceId) throws GBPersistenceException {
		try {
			List<Service> rs = esTemplate
					.queryForList(new NativeSearchQueryBuilder().withQuery(QueryBuilders.termQuery("id", serviceId))
							.withIndices("gb_apm_service").withTypes("service").build(), Service.class);
			if (rs.size() == 1) {
				return rs.get(0);
			}
			if (rs.size() > 1) {
				throw new GBPersistenceException(
						String.format("id[%d], Expected 1 but found %d", serviceId, rs.size()));
			}
			return null;
		} catch (Exception e) {
			throw new GBPersistenceException("查询service失败", e);
		}
	}

}
