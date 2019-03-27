package com.gb.apm.dao.es;

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
import com.gb.apm.kafka.stream.apm.common.dao.ServiceLinkDao;
import com.gb.apm.kafka.stream.apm.common.model.utils.JacksonUtils;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceLink;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceLinkMetaData;

@Repository
public class ServiceLinkDaoImpl implements ServiceLinkDao {
	
	private static final Logger log = LoggerFactory.getLogger(ServiceLinkDaoImpl.class);

	@Autowired
	private ElasticsearchTemplate esTemplate;

	@Override
	public void addServiceLink(ServiceLink link) throws GBPersistenceException {
		log.info("添加serviceLink到es");
		try {
			UpdateQueryBuilder upsert = new UpdateQueryBuilder();
			IndexRequest indexRequest = new IndexRequest("gb_apm_service_link", "service_link", link.getService().getId()+"").source(JacksonUtils.toJson(link),XContentType.JSON);
			upsert.withId(link.getService().getId()+"").withIndexName("gb_apm_service_link").withType("service_link").withDoUpsert(true)
				.withIndexRequest(indexRequest);
			esTemplate.update(upsert.build());
			log.info("添加serviceLink到es完成");
		} catch (Exception e) {
			throw new GBPersistenceException("添加service依赖关系失败", e);
		}
	}

	@Override
	public ServiceLinkMetaData findServiceLink(int key) throws GBPersistenceException {
		try {

		} catch (Exception e) {
			throw new GBPersistenceException("查询service依赖关系失败", e);
		}
		return null;
	}

	@Override
	public ServiceLink findServiceLinks(int serviceId) {
		return esTemplate.query(
				new NativeSearchQueryBuilder().withQuery(QueryBuilders.termQuery("sourceKey", serviceId))
						.withIndices("gb_apm_service_link").withTypes("service_link").withPageable(PageRequest.of(0, 10000)).build(),
				new ResultsExtractor<ServiceLink>() {
					@Override
					public ServiceLink extract(SearchResponse response) {
						for (SearchHit hit : response.getHits()) {
							try {
								return JacksonUtils.toObject(hit.getSourceAsString(), ServiceLink.class);
							} catch (Exception e) {
								e.printStackTrace();
								return null;
							}
						}
						return null;
					}
				});
	}

}
