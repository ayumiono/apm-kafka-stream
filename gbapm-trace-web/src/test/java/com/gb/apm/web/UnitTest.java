package com.gb.apm.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.ShardStats;
import org.elasticsearch.client.transport.TransportClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.gb.apm.web.controller.QueryTraceIdModel;
import com.gb.apm.web.controller.QueryTraceIdModel.Condition;
import com.gb.apm.web.controller.TraceController;
import com.gb.apm.web.timer.RolloverIndicesTimer;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApmWebApplication.class)
@ActiveProfiles("development")
public class UnitTest {
	
	public static final Logger log = LoggerFactory.getLogger(UnitTest.class);
	
	@Autowired
	RolloverIndicesTimer rolloverApi;
	
	@Autowired
	TraceController traceController;
	
	@Autowired
	TransportClient client;
	
	@Test
	public void rollover() {
		rolloverApi.rollover();
	}
	
	@Test
	public void expire() {
		rolloverApi.deleteExpiredIndices();
	}
	
	@Test
	public void relocatingAllShardsToOneNode() {
		rolloverApi.settingsBeforeShrinkIndex("active_gb_apm_trace-2018.11.20-000006");
	}
	
	@Test
	public void shrink() {
		try {
			String inactiveIndice = rolloverApi.shrinkIndex("active_gb_apm_trace-2018.11.20-000006");
			rolloverApi.monitorShrinkProcess(inactiveIndice);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void deleteOldIndex() {
		rolloverApi.deleteOldIndex("inactive_gb_apm_trace-2018.11.20-000006", "active_gb_apm_trace-2018.11.20-000006");
	}
	
	@Test
	public void indiceSegment() throws InterruptedException, ExecutionException {
//		ForceMergeResponse response = this.client.admin().indices().prepareForceMerge("inactive_gb_apm_trace-2018.11.13-000008").setMaxNumSegments(1).execute().get();
//		System.out.println(response.getTotalShards()+"  "+response.getSuccessfulShards()+"  "+response.getFailedShards());
		IndicesStatsResponse stats = client.admin().indices().prepareStats("inactive_gb_apm_trace-2018.11.13-000008").setSegments(true).get();
		for(ShardStats shard : stats.getShards()) {
			log.info("node id:{} isPrimary: {}, segmentCount:{}",shard.getShardRouting().currentNodeId(), shard.getShardRouting().primary(), shard.getStats().getSegments().getCount());
		}
		log.info("total segmentCount:{}",stats.getTotal().getSegments().getCount());
	}
	
	@Test
	public void queryBody() throws ParseException {
		QueryTraceIdModel model = new QueryTraceIdModel();
		SimpleDateFormat sf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date start = sf.parse("2018/11/21 16:16:09");
		Date end = sf.parse("2018/11/21 20:21:09");
		model.setTime_range(new Date[] {start,end});
		model.setAgentId(10);
		model.setPageIndex(0);
		model.setPageSize(5);
		Condition condition = new Condition();
		condition.setApi("sd_bl_so_tml_pay_hdr");
		condition.setArgs("");
		condition.setServiceType(2101);
		model.setConditions(Collections.singletonList(condition));
		traceController.trace(model);
	}
}
