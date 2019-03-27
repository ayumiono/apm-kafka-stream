package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ChainMetrics implements Serializable {

	private static final long serialVersionUID = -1835254013477613713L;
	private int invokeCount;
	private int successCount;
	private int failCount;
	private long averageSpent;
	private long minSpent = Long.MAX_VALUE;
	private String minSpentTraceId;
	private long maxSpent;
	private String maxSpentTraceId;

	private long startMs;
	private long endMs;

	private Map<Integer, Integer> distributeChart;

	private Set<Integer> xAxis;
	private Collection<Integer> yAxis;

	public Collection<Integer> getyAxis() {
		return distributeChart.values();
	}

	public Set<Integer> getxAxis() {
		return distributeChart.keySet();
	}

	public void distribute(long spent) {
		if (distributeChart == null) {
			distributeChart = new HashMap<>();
		}
		// 2位则除10，四舍五入取整后再乘10，3位则除100，以此类推
		double factor = Math.pow(10, String.valueOf(spent).length() - 1);
		BigDecimal s = new BigDecimal(spent);
		BigDecimal f = new BigDecimal(factor);
		int result = s.divide(f).setScale(0, BigDecimal.ROUND_HALF_EVEN).multiply(f).intValue();
		if (distributeChart.containsKey(result)) {
			distributeChart.put(result, distributeChart.get(result) + 1);
		} else {
			distributeChart.put(result, 1);
		}
	}

	public int getInvokeCount() {
		return invokeCount;
	}

	public void setInvokeCount(int invokeCount) {
		this.invokeCount = invokeCount;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	public int getFailCount() {
		return failCount;
	}

	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}

	public long getAverageSpent() {
		return averageSpent;
	}

	public void setAverageSpent(long averageSpent) {
		this.averageSpent = averageSpent;
	}

	public long getMinSpent() {
		return minSpent;
	}

	public void setMinSpent(long minSpent) {
		this.minSpent = minSpent;
	}

	public String getMinSpentTraceId() {
		return minSpentTraceId;
	}

	public void setMinSpentTraceId(String minSpentTraceId) {
		this.minSpentTraceId = minSpentTraceId;
	}

	public long getMaxSpent() {
		return maxSpent;
	}

	public void setMaxSpent(long maxSpent) {
		this.maxSpent = maxSpent;
	}

	public String getMaxSpentTraceId() {
		return maxSpentTraceId;
	}

	public void setMaxSpentTraceId(String maxSpentTraceId) {
		this.maxSpentTraceId = maxSpentTraceId;
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

	public Map<Integer, Integer> getDistributeChart() {
		return distributeChart;
	}

	public void setDistributeChart(Map<Integer, Integer> distributeChart) {
		this.distributeChart = distributeChart;
	}
}
