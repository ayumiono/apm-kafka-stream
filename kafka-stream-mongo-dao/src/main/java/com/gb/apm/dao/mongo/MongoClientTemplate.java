package com.gb.apm.dao.mongo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;

@Component
@ConfigurationProperties(prefix="mongo")
public class MongoClientTemplate implements InitializingBean{
	
	private static final Logger logger = LoggerFactory.getLogger(MongoClientTemplate.class);
	
	private MongoClient client;
	
	private Map<String, Object> conf;
	
	public MongoClient client() {
		return this.client;
	}

	public Map<String, Object> getConf() {
		return conf;
	}

	public void setConf(Map<String, Object> conf) {
		this.conf = conf;
	}
	
	private void init() {
		logger.info("mogo conf:{}",conf);
		MongoClientOptions.Builder build = new MongoClientOptions.Builder();
		build.connectionsPerHost(Integer.parseInt(conf.get("max.connections.per.host").toString()));
		build.connectTimeout(Integer.parseInt(conf.get("connect.timeout").toString()));
		build.maxWaitTime(Integer.parseInt(conf.get("max.wait.time").toString()) * 60 * 1000);
		build.maxConnectionIdleTime(Integer.parseInt(conf.get("max.connection.idletime").toString()) * 60 * 1000);
		build.minConnectionsPerHost(Integer.parseInt(conf.get("min.connections.per.host").toString()));
		MongoClientOptions option = build.build();
		this.client = new MongoClient(conf.get("host").toString(), option);
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		init();
	}
}
