package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;

public class JVMGCMetrics implements Serializable{
	private static final long serialVersionUID = 6340961418988869480L;
	private String gcAlgorithm;
	private long collectionCount;
	private long collectionTime;
	
	public JVMGCMetrics(String gcAlgorithm,long collectionCount,long collectionTime) {
		this.gcAlgorithm = gcAlgorithm;
		this.collectionCount = collectionCount;
		this.collectionTime = collectionTime;
	}
	
	public JVMGCMetrics() {}

	public String getGcAlgorithm() {
		return gcAlgorithm;
	}

	public void setGcAlgorithm(String gcAlgorithm) {
		this.gcAlgorithm = gcAlgorithm;
	}

	public long getCollectionCount() {
		return collectionCount;
	}

	public void setCollectionCount(long collectionCount) {
		this.collectionCount = collectionCount;
	}

	public long getCollectionTime() {
		return collectionTime;
	}

	public void setCollectionTime(long collectionTime) {
		this.collectionTime = collectionTime;
	}
}
