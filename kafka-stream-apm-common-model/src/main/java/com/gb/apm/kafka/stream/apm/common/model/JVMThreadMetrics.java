package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;

public class JVMThreadMetrics implements Serializable{
	private static final long serialVersionUID = -8671208761821894618L;
	private int peakThreadCount;
	private int threadCount;
	private long totalStartedThreadCount;
	
	public JVMThreadMetrics() {}
	
	public int getPeakThreadCount() {
		return peakThreadCount;
	}

	public void setPeakThreadCount(int peakThreadCount) {
		this.peakThreadCount = peakThreadCount;
	}

	public int getThreadCount() {
		return threadCount;
	}

	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public long getTotalStartedThreadCount() {
		return totalStartedThreadCount;
	}

	public void setTotalStartedThreadCount(long totalStartedThreadCount) {
		this.totalStartedThreadCount = totalStartedThreadCount;
	}

	public JVMThreadMetrics(int peakThreadCount,int threadCount,long totalStartedThreadCount) {
		this.peakThreadCount = peakThreadCount;
		this.threadCount = threadCount;
		this.totalStartedThreadCount = totalStartedThreadCount;
	}
}
