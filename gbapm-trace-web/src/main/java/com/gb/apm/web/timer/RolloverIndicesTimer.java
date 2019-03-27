package com.gb.apm.web.timer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.alias.exists.AliasesExistResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.rollover.RolloverResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.shrink.ResizeResponse;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * 详细流程见src/template/active_gb_apm_trace.json
 * @author xuelong.chen
 */
@Component
@EnableScheduling
public class RolloverIndicesTimer implements InitializingBean {
	
	public static final String ALIAS_ACTIVE_GB_APM_TRACE = "gb_apm_trace";
	public static final String ALIAS_SEARCH_GB_APM_TRACE = "search_gb_apm_trace";
	
	private static final String TEMPLATE_NAME_ACTIVE_GB_APM_TRACE = "active_gb_apm_trace";
	private static final String TEMPLATE_NAME_INACTIVE_GB_APM_TRACE = "inactive_gb_apm_trace";
	
	private static final Logger log = LoggerFactory.getLogger(RolloverIndicesTimer.class);
	
	@Value("${indices.clear.interval.days:7}")
	private int clearIntervalDays;
	
	@Value("${inactive.primary.shards:1}")
	private String inactivePrimaryShards;
	
	@Value("${active.primary.shards:3}")
	private String activePrimaryShards;
	
	@Value("${inactive.replica.shards:1}")
	private int inactiveReplicaShards;
	
	@Value("${active.replica.shards:1}")
	private String activeReplicaShards;
	
	@Value("${inactive.indices.node:random}")
	private String inactiveIndicesNode;
	
	@Value("${gbapm.service.primary.shards:1}")
	private String gbapmServicePrimaryShards;
	
	@Value("${gbapm.service.replica.shards:2}")
	private String gbapmServiceReplicaShards;
	
	@Value("${es.request.retry.times:3}")
	private int esRequestRetryTimes;
	
	@Value("${relocating.monitor.timeout:30}")
	private int monitorSettingsBeforeShrinkIndexTimeout;
	
	@Value("${es.request.retry.interval:2}") 
	private int esRequestRetryInterval;
	
	@Autowired
	private TransportClient client;
	
	private volatile boolean stop = false;
	
	/**
	 * invoke 00:30 everyday
	 */
	@Scheduled(cron="0 30 0 * * ?")
	public void rollover() {
		if(stop) {
			log.info("timer has stoped!");
			return;
		}
		log.info("start rollover process...");
		String oldIndex = "";
		try {
			oldIndex = this.rolloverIndex();
			if(oldIndex == null) {
				log.info("fail rollover condition, skip the current rollover job.");
				return;
			}
		} catch (Exception e) {
			log.error("rolloverIndex failed as _rollover request failed!",e);
			return;
		}
		log.info("start shrink process...");//optimize read and write, not necessary
		String inactiveIndex = null;
		try {
			if(!this.settingsBeforeShrinkIndex(oldIndex)) {
				return;
			}
			inactiveIndex = this.shrinkIndex(oldIndex);
			if(!this.monitorShrinkProcess(inactiveIndex)) {
				return;
			}
			if(!this.modifyAlias(oldIndex, inactiveIndex)) {
				return;
			}
		} catch (Exception e) {
			log.warn("kick shrink index process failed!",e);
			return;
		}
		log.info("shrink process completed. inactiveIndex: {}", inactiveIndex);
		
		log.info("start force merge process...");
		try {
			if(!this.forceMerge(inactiveIndex)) {//save space, not necessary
				return;
			}
			this.deleteOldIndex(inactiveIndex,oldIndex);
		} catch (Exception e) {
			return;
		}
		log.info("force merge process completed.");
	}
	
	/**
	 * invoke 00:00 every day
	 * Deleting old indices
	 */
	@Scheduled(cron="0 0 0 * * ?")
	public void deleteExpiredIndices() {
		if(stop) {
			log.info("timer has stoped!");
			return;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		long now = System.currentTimeMillis();
		log.info("start delete expired indices process...");
		try {
			GetIndexResponse response = this.client.admin().indices().prepareGetIndex().setIndices(TEMPLATE_NAME_INACTIVE_GB_APM_TRACE+"-*").get();
			log.info("query inactive indices finished, start process each match single indice...");
			String[] indices = response.getIndices();
			ImmutableOpenMap<String, Settings> settings = response.getSettings();
			for(String indice : indices) {
				try {
					Settings setting = settings.get(indice);
					long createTimeStamp = setting.getAsLong("index.creation_date", 0L);
					log.info("create time of index: {} is {} {}",indice,createTimeStamp,sdf.format(new Date(createTimeStamp)));
					if(now - createTimeStamp >= this.clearIntervalDays*24*60*60*1000) {
						log.info("start delete expire index {}",indice);
						DeleteIndexResponse resp = this.client.admin().indices().prepareDelete(indice).get();
						if(resp.isAcknowledged()) {
							log.info("delete done!");
						}
					}
				} catch (Exception e) {
					log.error(indice+"delete failed!",e);
				}
			}
			log.info("each match indice process finished");
			
			GetAliasesResponse aliasesResp = this.client.admin().indices().prepareGetAliases("gb_apm_trace").get();
			Set<String> tmp = new HashSet<>();
			String[] tmpArray = aliasesResp.getAliases().keys().toArray(String.class);
			tmp.addAll(Arrays.asList(tmpArray));
			log.info("current hot indices:");
			tmp.stream().forEach(new Consumer<String>() {
				@Override
				public void accept(String t) {
					log.info(t);
				}
			});
			response = this.client.admin().indices().prepareGetIndex().setIndices(TEMPLATE_NAME_ACTIVE_GB_APM_TRACE+"-*").get();
			log.info("query active indices finished, start process each match single indice...");
			indices = response.getIndices();
			settings = response.getSettings();
			for(String indice : indices) {
				if(tmp.contains(indice)) continue;//do not delete the current active write indice
				try {
					Settings setting = settings.get(indice);
					long createTimeStamp = setting.getAsLong("index.creation_date", 0L);
					log.info("create time of index: {} is {} {}",indice,createTimeStamp,sdf.format(new Date(createTimeStamp)));
					if(now - createTimeStamp >= this.clearIntervalDays*24*60*60*1000) {
						log.info("start delete expire index {}",indice);
						DeleteIndexResponse resp = this.client.admin().indices().prepareDelete(indice).get();
						if(resp.isAcknowledged()) {
							log.info("delete done!");
						}
					}
				} catch (Exception e) {
					log.error(indice+"delete failed!",e);
				}
			}
			log.info("each match indice process finished");
		} catch (Exception e) {
			log.error("delete expired indices failed!",e);
		}
		
		log.info("delete expired indices process completed.");
	}
	
	/**
	 * Rolling over the index
	 */
	private String rolloverIndex() throws Exception {
		int attempt = 0;
		while(true) {
			try {
				RolloverResponse response = this.client.admin().indices().prepareRolloverIndex(ALIAS_ACTIVE_GB_APM_TRACE)
						.get();
				if(!response.isRolledOver()) {
					return null;
				}
				String oldIndex = response.getOldIndex();
				String newIndex = response.getNewIndex();
				log.info("rollover process completed. oldIndex:{} newIndex:{}", oldIndex, newIndex);
				return oldIndex;
			} catch (Exception e) {
				log.error("rollover process failed as _rollover request failed!",e);
				try {
					Thread.sleep(this.esRequestRetryInterval*1000);
				} catch (InterruptedException e1) {
					log.error("interrupted exception!",e1);
				}
				attempt+=1;
				log.info("start {} attempt",attempt);
				if(attempt >= esRequestRetryTimes) {
					log.error("rollover failed after {} attempts",attempt-1);
					throw e;
				}
			}
		}
	}
	
	/**
	 * attention: this step maybe take a long time!!!
	 * @param oldIndex
	 * @return
	 */
	public boolean settingsBeforeShrinkIndex(String oldIndex) {
		Map<String, Object> settings = new HashMap<>();
//		settings.put("index.routing.allocation.total_shards_per_node", 3);//FIXME
		settings.put("index.blocks.write", true);
		try {
			if(this.inactiveIndicesNode.equals("random")) {
				NodesInfoResponse resp = this.client.admin().cluster().prepareNodesInfo().get();//实时查询结点
				List<String> candidateNodes = new ArrayList<>();
				for(NodeInfo nodeInfo : resp.getNodes()) {
					boolean dataNode = nodeInfo.getSettings().getAsBoolean("node.data", false);
					String nodeName = nodeInfo.getSettings().get("node.name");
					if(dataNode) {
						candidateNodes.add(nodeName);
					}
				}
				if(candidateNodes.size() == 0) {
					return false;//如果没有符合的节点，则跳过优化
				}
				String _name = candidateNodes.get(RandomUtils.nextInt(0, candidateNodes.size()));//随机选取一个dataNode合并分片
				log.info("chose datanode {} to merge all shards of {}",_name,oldIndex);
				settings.put("index.routing.allocation.require._name", _name);
			}else {
				if(StringUtils.isBlank(this.inactiveIndicesNode)) {
					log.error("inactive.indices.node is blank");
					return false;
				}else {
					settings.put("index.routing.allocation.require._name", this.inactiveIndicesNode);
				}
			}
			UpdateSettingsResponse response = this.client.admin().indices().prepareUpdateSettings()
					.setIndices(oldIndex)
					.setSettings(settings)
					.get();
			if(!response.isAcknowledged()) {
				return false;
			}
			int retryTimes = 0;
			while(true) {// try 3 times
				//wait 30s default for oldindex has finished relocating
				ClusterHealthResponse reps = this.client.admin().cluster().prepareHealth()
						.setIndices(oldIndex).setWaitForNoRelocatingShards(true).setTimeout(TimeValue.timeValueSeconds(this.monitorSettingsBeforeShrinkIndexTimeout)).get();
				if(reps.getRelocatingShards() == 0) {
					log.info("relocate old indice to inactive indice process finished.");
					return true;
				}else {
					log.info("relocate process not finished yet, wait more minutes");
					retryTimes += 1;
					if(retryTimes >= esRequestRetryTimes) {
						log.info("monitor relocate process more than 3 times, but still failed!");
						break;
					}
					log.info("monitor relocate process again!");
				}
			}
		} catch (Exception e) {
			log.warn("settings before shrink index failed!",e);
		}
		return false;
	}
	
	/**
	 * Shrinking the index
	 */
	public String shrinkIndex(String oldIndex) throws Exception {
		String sufix = StringUtils.substringAfter(oldIndex, "-");
		String inactiveIndex = TEMPLATE_NAME_INACTIVE_GB_APM_TRACE+"-" + sufix;
		ResizeResponse response = this.client.admin().indices().prepareResizeIndex(oldIndex, inactiveIndex).get();
		return response.index();
	}
	
	
	public boolean monitorShrinkProcess(String inactiveIndice) {
		try {
			int retryTimes = 0;
			while(true) {// try 3 times
				ClusterHealthResponse response = this.client.admin().cluster()
						.prepareHealth(inactiveIndice)
						.setWaitForYellowStatus()
						.get();
				if(response.getStatus() == ClusterHealthStatus.YELLOW || response.getStatus() == ClusterHealthStatus.GREEN) {
					return true;
				}else {
					log.info("shrink process not finished yet, wait more minutes");
					retryTimes+=1;
					if(retryTimes >= esRequestRetryTimes) {
						log.info("wait for shrink process finish more than 3 times, but still failed!");
						break;
					}
					log.info("try monitor shrink process again!");
				}
				
			}
		} catch (Exception e) {
			log.warn("shrink process failed!", e);
		}
		return false;
	}
	
	private boolean modifyAlias(String oldIndex, String shrinkIndex) {
		try {
			IndicesAliasesResponse response = this.client.admin().indices().prepareAliases()
				.addAlias(shrinkIndex, ALIAS_SEARCH_GB_APM_TRACE)
				.removeAlias(oldIndex, ALIAS_SEARCH_GB_APM_TRACE)
				.get();
			return response.isAcknowledged();
		} catch (Exception e) {
			log.warn("modify index alias failed!", e);
		}
		return false;
	}
	
	/**
	 * Saving space
	 */
	private boolean forceMerge(String inactiveIndex) {
		try {
			int retryTimes = 0;
			while(true) {
				ForceMergeResponse response = this.client.admin().indices().prepareForceMerge(inactiveIndex).setMaxNumSegments(1).get();
				if(response.getTotalShards() == response.getSuccessfulShards() && response.getFailedShards() == 0) {
					log.info("_forcemerge api success.");
					break;
				}else {
					log.info("totalShards:{} successfullShards:{} failedShards:{}", response.getTotalShards(),response.getSuccessfulShards(),response.getFailedShards());
					DefaultShardOperationFailedException[] failures = response.getShardFailures();
					for(DefaultShardOperationFailedException failure : failures) {
						log.info("forcemerge request execute failed! index:{} shard:{} reason:{}",failure.index(),failure.shardId(),failure.reason());
					}
					retryTimes+=1;
					if(retryTimes >= esRequestRetryTimes) {
						log.info("try forcemerge 3 times, but still failed!");
						return false;
					}
					Thread.sleep(this.esRequestRetryInterval*1000);
					log.info("try forcemerge again!");
				}
			}
			//unnecessary
			UpdateSettingsResponse resp = this.client.admin().indices().prepareUpdateSettings()
				.setIndices(inactiveIndex).setSettings(Collections.singletonMap("number_of_replicas", this.inactiveReplicaShards))
				.get();
			return resp.isAcknowledged();
		} catch (Exception e) {
			log.warn("force merge cold indices failed!",e);
		}
		return false;
	}
	
	/**
	 * 这一步失败不重要，会在定时任务中删除索引
	 * @param oldIndex
	 * @return
	 */
	public boolean deleteOldIndex(String inactiveIndex, String oldIndex) {
		try {
			int retryTimes = 0;
			while(true) {// try 3 times
				ClusterHealthResponse rsp = this.client.admin().cluster()
						.prepareHealth(inactiveIndex)
						.setWaitForGreenStatus()
						.get();
				if(rsp.getStatus() == ClusterHealthStatus.GREEN) {
					break;
				}else {
					retryTimes+=1;
					if(retryTimes >= this.esRequestRetryTimes) {
						return false;
					}
				}
			}
			DeleteIndexResponse response = this.client.admin().indices().prepareDelete(oldIndex).get();
			return response.isAcknowledged();
		} catch (Exception e) {
			log.error("delete old index failed!",e);
		}
		return false;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		indicesTemplateCheck();
		indicesCheck();
		otherIndicesCheck();
	}
	
	public void cleanEs() throws Exception {
		try {
			//clear template
			this.client.admin().indices().prepareDeleteTemplate(TEMPLATE_NAME_ACTIVE_GB_APM_TRACE).get();
			this.client.admin().indices().prepareDeleteTemplate(TEMPLATE_NAME_INACTIVE_GB_APM_TRACE).get();
			//clean indices
			this.client.admin().indices().prepareDelete(ALIAS_ACTIVE_GB_APM_TRACE+"-*").get();
			this.client.admin().indices().prepareDelete(ALIAS_SEARCH_GB_APM_TRACE+"-*").get();
		} catch (Exception e) {
			throw e;
		}
	}
	
	public void stop() {
		this.stop = true;
	}
	
	public void start() {
		this.stop = false;
	}
	
	private void otherIndicesCheck() {
		//gb_apm_service
		try {
			IndicesExistsResponse resp = this.client.admin().indices().prepareExists("gb_apm_service").get();
			if(!resp.isExists()) {
				Map<String, String> source = new HashMap<>();
				source.put("index.blocks.read_only_allow_delete", "false");
				source.put("number_of_shards", this.gbapmServicePrimaryShards);
				source.put("number_of_replicas", this.gbapmServiceReplicaShards);
				CreateIndexResponse response = this.client.admin().indices().prepareCreate("gb_apm_service").setSettings(source).get();
				if(!response.isAcknowledged()) {
					log.error("create gb_apm_service indice failed! please create manually");
				}
			}
		} catch (Exception e) {
			log.error("create gb_apm_service indice failed! please create manually",e);
		}
		//gb_apm_service_link
		try {
			IndicesExistsResponse resp = this.client.admin().indices().prepareExists("gb_apm_service_link").get();
			if(!resp.isExists()) {
				Map<String, String> source = new HashMap<>();
				source.put("index.blocks.read_only_allow_delete", "false");
				source.put("number_of_shards", this.gbapmServicePrimaryShards);
				source.put("number_of_replicas", this.gbapmServiceReplicaShards);
				CreateIndexResponse response = this.client.admin().indices().prepareCreate("gb_apm_service_link").setSettings(source).get();
				if(!response.isAcknowledged()) {
					log.error("create gb_apm_service_link indice failed! please create manually");
				}
			}
		} catch (Exception e) {
			log.error("create gb_apm_service_link indice failed! please create manually",e);
		}
	}
	
	/**
	 * 检测es是否有active_gb_apm_trace,inactive_gb_apm_trace索引模版，如果没有，则创建
	 */
	private void indicesTemplateCheck() throws Exception {
		try {
			GetIndexTemplatesResponse response = this.client.admin().indices().prepareGetTemplates(TEMPLATE_NAME_ACTIVE_GB_APM_TRACE).get();
			List<IndexTemplateMetaData> templates = response.getIndexTemplates();
			if(templates.size() == 0) {
				log.info(TEMPLATE_NAME_ACTIVE_GB_APM_TRACE + " template not exist, create now...");
				this.client.admin().indices().preparePutTemplate(TEMPLATE_NAME_ACTIVE_GB_APM_TRACE)
				.setPatterns(Collections.singletonList(TEMPLATE_NAME_ACTIVE_GB_APM_TRACE+"-*"))
				.addAlias(new Alias(ALIAS_SEARCH_GB_APM_TRACE))
				.setSettings(activeLogsSettings())
				.addMapping("trace", mapping()).get();
				log.info(TEMPLATE_NAME_ACTIVE_GB_APM_TRACE + " template created");
			}
			response = this.client.admin().indices().prepareGetTemplates(TEMPLATE_NAME_INACTIVE_GB_APM_TRACE).get();
			templates = response.getIndexTemplates();
			if(templates.size() == 0) {
				log.info(TEMPLATE_NAME_INACTIVE_GB_APM_TRACE + " template not exist, create now...");
				this.client.admin().indices().preparePutTemplate(TEMPLATE_NAME_INACTIVE_GB_APM_TRACE)
				.setPatterns(Collections.singletonList(TEMPLATE_NAME_INACTIVE_GB_APM_TRACE+"-*"))
				.setSettings(inactiveLogsSettings())
				.addMapping("trace", mapping()).get();
				log.info(TEMPLATE_NAME_INACTIVE_GB_APM_TRACE + " template created");
			}
		} catch (Exception e) {
			log.error("indices template check failed",e);
			throw e;
		}
	}
	
	/**
	 * 检查es是否有active_gb_apm_trace索引，如果没有，则创建一个active_gb_apm_trace-{now/d}-1索引
	 */
	private void indicesCheck() throws Exception {
		try {
			AliasesExistResponse response = this.client.admin().indices().prepareAliasesExist(ALIAS_ACTIVE_GB_APM_TRACE).get();
			if(!response.isExists()) {
				String indiceName = "<" + TEMPLATE_NAME_ACTIVE_GB_APM_TRACE + "-{now/d}-000001>";
				CreateIndexResponse createIndexResponse = this.client.admin().indices().prepareCreate(indiceName).addAlias(new Alias(ALIAS_ACTIVE_GB_APM_TRACE)).get();
				if(!createIndexResponse.isAcknowledged()) {
					throw new Exception("init indice failed!");
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error("indices check failed",e);
			throw e;
		}
	}
	
	private Settings activeLogsSettings() throws Exception {
		Map<String,String> _source = new HashMap<>();
		_source.put("number_of_shards", this.activePrimaryShards);
		_source.put("number_of_replicas", this.activeReplicaShards);
		_source.put("index.blocks.read_only_allow_delete", "false");
//		_source.put("routing.allocation.total_shards_per_node", "1");FIXME
		_source.put("analysis.analyzer.gb_apm_analyzer.type", "pattern");
		Settings settings = Settings.builder().putProperties(_source, Function.identity()).build();
		return settings;
	}
	
	private Settings inactiveLogsSettings() throws Exception {
		Map<String,String> _source = new HashMap<>();
		_source.put("number_of_shards", this.inactivePrimaryShards);
		_source.put("number_of_replicas", "0");
		_source.put("codec", "best_compression");
		_source.put("analysis.analyzer.gb_apm_analyzer.type", "pattern");
		Settings settings = Settings.builder().putProperties(_source, Function.identity()).build();
		return settings;
	}
	
	private XContentBuilder mapping() throws IOException {
		XContentBuilder mapping = XContentFactory.jsonBuilder().startObject()
		.startObject("properties")
				.startObject("agentId").field("type", "keyword").endObject()
				.startObject("agentStartTime").field("type", "long").endObject()
				.startObject("applicationName").field("type", "keyword").endObject()
				.startObject("applicationServiceType").field("type", "integer").endObject()
				.startObject("elapsed").field("type", "integer").endObject()
				.startObject("endPoint").field("type", "text").field("analyzer","gb_apm_analyzer").endObject()
				.startObject("err").field("type", "integer").endObject()
				.startObject("remoteAddr").field("type", "text").field("analyzer","gb_apm_analyzer").endObject()
				.startObject("rpc").field("type", "text").field("analyzer","gb_apm_analyzer").endObject()
				.startObject("serviceType").field("type", "integer").endObject()
				.startObject("spanId").field("type", "long").endObject()
				.startObject("startTime").field("type", "long").endObject()
				.startObject("transactionId").field("type", "keyword").endObject()
				.startObject("annotations").field("type", "nested")
					.startObject("properties")
						.startObject("key").field("type", "integer").endObject()
						.startObject("value").field("type", "text").field("analyzer","gb_apm_analyzer").endObject()
					.endObject()
				.endObject()
				.startObject("exceptionInfo")
					.startObject("properties")
						.startObject("intValue").field("type", "integer").endObject()
						.startObject("stringValue").field("type", "text").field("analyzer","gb_apm_analyzer").endObject()
					.endObject()
				.endObject()
				.startObject("tspanEventList").field("type", "nested")
					.startObject("properties")
						.startObject("afterTime").field("type", "long").endObject()
						.startObject("agentId").field("type", "keyword").endObject()
						.startObject("depth").field("type", "integer").endObject()
						.startObject("endElapsed").field("type", "integer").endObject()
						.startObject("parentSequence").field("type", "integer").endObject()
						.startObject("sequence").field("type", "integer").endObject()
						.startObject("serviceType").field("type", "integer").endObject()
						.startObject("startElapsed").field("type", "integer").endObject()
						.startObject("startTime").field("type", "long").endObject()
						.startObject("transactionId").field("type", "keyword").endObject()
						.startObject("annotations").field("type", "nested")
							.startObject("properties")
								.startObject("key").field("type", "integer").endObject()
								.startObject("value").field("type", "text").field("analyzer","gb_apm_analyzer").endObject()
							.endObject()
						.endObject()
					.endObject()
				.endObject()
		.endObject().endObject();
		return mapping;
	}
}
