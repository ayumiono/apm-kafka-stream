package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.gb.apm.kafka.stream.apm.common.annotation.MCollection;
import com.gb.apm.kafka.stream.apm.common.annotation.MID;

@MCollection("apm_chain_tree")
public class Chain implements Serializable {
	private static final long serialVersionUID = 2901762949515610476L;
	
	@MID
	private Long id;
	/**
	 * 用来计算链路是否变化
	 */
	private Long hashCode;
	private List<TreeStackFrame> stacks;
	
	private Date createTime;
	private Date updateTime;
	private Integer systemId;
	private String systemName;
	
	private int type;
	private String rpcStubMethodDesc;
	
	private String entranceMethodDesc;
	
	private String entranceMethod;
	
	public Chain() {}
	
	public Chain(long id,List<TreeStackFrame> stacks,long hashCode,
			int systemId,String systemName,String entranceMethod,
			String entranceMethodDesc,int type,String rpcStubMethodDesc) {
		this.id = id;
		this.stacks = stacks;
		this.hashCode = hashCode;
		this.createTime = new Date();
		this.updateTime = new Date();
		this.systemId = systemId;
		this.systemName = systemName;
		this.entranceMethod = entranceMethod;
		this.entranceMethodDesc = entranceMethodDesc;
		this.type = type;
		this.rpcStubMethodDesc = rpcStubMethodDesc;
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getHashCode() {
		return hashCode;
	}
	public void setHashCode(long hashCode) {
		this.hashCode = hashCode;
	}
	public List<TreeStackFrame> getStacks() {
		return stacks;
	}
	public void setStacks(List<TreeStackFrame> stacks) {
		this.stacks = stacks;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Integer getSystemId() {
		return systemId;
	}

	public void setSystemId(Integer systemId) {
		this.systemId = systemId;
	}

	public String getSystemName() {
		return systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getRpcStubMethodDesc() {
		return rpcStubMethodDesc;
	}

	public void setRpcStubMethodDesc(String rpcStubMethodDesc) {
		this.rpcStubMethodDesc = rpcStubMethodDesc;
	}

	public String getEntranceMethodDesc() {
		return entranceMethodDesc;
	}

	public void setEntranceMethodDesc(String entranceMethodDesc) {
		this.entranceMethodDesc = entranceMethodDesc;
	}

	public String getEntranceMethod() {
		return entranceMethod;
	}

	public void setEntranceMethod(String entranceMethod) {
		this.entranceMethod = entranceMethod;
	}

	@Override
	public String toString() {
		return "Chain [id=" + id + ", hashCode=" + hashCode + 
				", systemId=" + systemId + ", systemName=" + systemName + ", type="
				+ type + ", rpcStubMethodDesc=" + rpcStubMethodDesc + ", entranceMethodDesc=" + entranceMethodDesc
				+ ", entranceMethod=" + entranceMethod + "]";
	}
}
