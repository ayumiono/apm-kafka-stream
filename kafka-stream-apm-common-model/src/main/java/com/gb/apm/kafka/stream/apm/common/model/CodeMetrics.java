package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * code指标分析
 * @author xuelong.chen
 *
 */
public class CodeMetrics implements Serializable{
	private static final long serialVersionUID = -8126354785887435102L;
	private int systemId;
	private Map<Long, Long> counts = new HashMap<>();//map的key不要存null，否则fastjson会解析错误
	private long startMs;
	private long endMs;
	private long nullCounter;
	public int getSystemId() {
		return systemId;
	}
	public void setSystemId(int systemId) {
		this.systemId = systemId;
	}
	
	public void inc(Long code) {
		if(code == null) {
			nullCounter += 1;
			return;
		}
		if(counts.containsKey(code)) {
			counts.put(code, counts.get(code)+1);
		}else {
			counts.put(code, 1L);
		}
	}
	public Map<Long, Long> getCounts() {
		return counts;
	}
	public void setCounts(Map<Long, Long> counts) {
		this.counts = counts;
	}
	public long getStartMs() {
		return startMs;
	}
	public void setStartMs(long startMs) {
		this.startMs = startMs;
	}
	public long getEndMs() {
		return endMs;
	}
	public void setEndMs(long endMs) {
		this.endMs = endMs;
	}
	public long getNullCounter() {
		return nullCounter;
	}
	public void setNullCounter(long nullCounter) {
		this.nullCounter = nullCounter;
	}
	@Override
	public String toString() {
		return "CodeMetrics [systemId=" + systemId + ", counts=" + counts + ", startMs=" + startMs + ", endMs=" + endMs
				+ "]";
	}
}
