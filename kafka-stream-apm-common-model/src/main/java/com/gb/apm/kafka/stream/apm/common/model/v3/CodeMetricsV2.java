package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * code指标分析
 * FIXME 
 * @author xuelong.chen
 *
 */
public class CodeMetricsV2 implements Serializable {
	private static final long serialVersionUID = -8126354785887435102L;
	private String agentId;
	private Map<Long, Long> counts = new HashMap<>();// map的key不要存null，否则fastjson会解析错误
	private Map<Long, List<String>> exceptionTransactionids = new HashMap<>();// 错误关联traceId
	private long startMs;
	private long endMs;
	private long nullCounter;

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public Map<Long, List<String>> getExceptionTransactionids() {
		return exceptionTransactionids;
	}

	public void setExceptionTransactionids(Map<Long, List<String>> exceptionTransactionids) {
		this.exceptionTransactionids = exceptionTransactionids;
	}

	public void inc(Long code, String transactionId) {
		if (code == null) {
			nullCounter += 1;
			return;
		}
		if (counts.containsKey(code)) {
			counts.put(code, counts.get(code) + 1);
		} else {
			counts.put(code, 1L);
		}

		if (exceptionTransactionids.containsKey(code)) {
			exceptionTransactionids.get(code).add(transactionId);
		} else {
			exceptionTransactionids.put(code, Collections.singletonList(transactionId));
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
		return "CodeMetrics [agentId=" + agentId + ", counts=" + counts + ", startMs=" + startMs + ", endMs=" + endMs
				+ "]";
	}
}
