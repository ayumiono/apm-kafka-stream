package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;

public class ChainTopMetrics implements Serializable,Comparable<ChainTopMetrics> {

	private static final long serialVersionUID = 4788005958105726836L;

	private long averageSpent;

	private int systemId;
	private long chainId;
	private String entranceMethodDesc;
	private String rpcStubMethodDesc;
	private int type;
	
	public ChainTopMetrics() {}
	
	public ChainTopMetrics(int systemId,long chainId,String entranceMethodDesc,String rpcStubMethodDesc,int type) {
		this.systemId = systemId;
		this.chainId = chainId;
		this.entranceMethodDesc = entranceMethodDesc;
		this.rpcStubMethodDesc = rpcStubMethodDesc;
		this.type = type;
	}
	
	public long getAverageSpent() {
		return averageSpent;
	}

	public void setAverageSpent(long averageSpent) {
		this.averageSpent = averageSpent;
	}

	public long getChainId() {
		return chainId;
	}

	public void setChainId(long chainId) {
		this.chainId = chainId;
	}

	public int getSystemId() {
		return systemId;
	}

	public void setSystemId(int systemId) {
		this.systemId = systemId;
	}

	public String getEntranceMethodDesc() {
		return entranceMethodDesc;
	}

	public void setEntranceMethodDesc(String entranceMethodDesc) {
		this.entranceMethodDesc = entranceMethodDesc;
	}

	public String getRpcStubMethodDesc() {
		return rpcStubMethodDesc;
	}

	public void setRpcStubMethodDesc(String rpcStubMethodDesc) {
		this.rpcStubMethodDesc = rpcStubMethodDesc;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object that) {
		if(! (that instanceof ChainTopMetrics)) {
			return false;
		}
		return String.valueOf(this.chainId).equals(String.valueOf(((ChainTopMetrics)that).chainId));
	}

	@Override
	public int compareTo(ChainTopMetrics that) {
		return (int)(averageSpent - that.averageSpent);
	}
}
