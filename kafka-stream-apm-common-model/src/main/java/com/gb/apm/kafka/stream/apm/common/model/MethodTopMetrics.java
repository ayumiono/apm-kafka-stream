package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;

public class MethodTopMetrics implements Serializable,Comparable<MethodTopMetrics>{

	private static final long serialVersionUID = -4904948589339765115L;
	
	private long averageSpent;
	
	private int systemId;
	private String methodDesc;
	
	private long id;//有两种可能的值：方法级别ID，链路级别ID
	private long chainId;//可为空，代表方法级别方法，不为空时代表链路级别方法
	
	public long getAverageSpent() {
		return averageSpent;
	}

	public void setAverageSpent(long averageSpent) {
		this.averageSpent = averageSpent;
	}

	public int getSystemId() {
		return systemId;
	}

	public void setSystemId(int systemId) {
		this.systemId = systemId;
	}

	public String getMethodDesc() {
		return methodDesc;
	}

	public void setMethodDesc(String methodDesc) {
		this.methodDesc = methodDesc;
	}

	public long getChainId() {
		return chainId;
	}

	public void setChainId(long chainId) {
		this.chainId = chainId;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public MethodTopMetrics() {}
	
	public MethodTopMetrics(long id,int systemId,String methodDesc) {
		this.id = id;
		this.systemId = systemId;
		this.methodDesc = methodDesc;
	}

	@Override
	public int compareTo(MethodTopMetrics o) {
		return (int)(averageSpent - o.averageSpent);
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof MethodTopMetrics) {
			if(String.valueOf(((MethodTopMetrics)o).id).equals(String.valueOf(id))) {
				return true;
			}
		}
		return false;
	}
}
