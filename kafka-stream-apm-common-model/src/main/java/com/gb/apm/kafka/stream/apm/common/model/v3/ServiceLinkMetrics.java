package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.util.Map;

import com.gb.apm.model.TSpan;

/**
 * 
 * 统计vizceral connection数据
 * @author xuelong.chen
 *
 */
public class ServiceLinkMetrics {
	private String source;
	private Map<String, Map<String,Integer>> targetMetrics;
	
	public void hit(TSpan span) {
		
	}
}
