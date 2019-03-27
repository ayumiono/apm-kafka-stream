package com.gb.apm.kafka.stream.apm.common.model;

import java.util.Date;

public class SystemInfo {
	private long systemNumId;
	private int tenantNumId;
	private int dataSign;
	private String systemName;
	private Date createDtme;
	private Date lastUpdtme;
	private long createUserId;
	private long lastUpdateUserId;
	private String datasourceName;
	public long getSystemNumId() {
		return systemNumId;
	}
	public void setSystemNumId(long systemNumId) {
		this.systemNumId = systemNumId;
	}
	public int getTenantNumId() {
		return tenantNumId;
	}
	public void setTenantNumId(int tenantNumId) {
		this.tenantNumId = tenantNumId;
	}
	public int getDataSign() {
		return dataSign;
	}
	public void setDataSign(int dataSign) {
		this.dataSign = dataSign;
	}
	public String getSystemName() {
		return systemName;
	}
	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}
	public Date getCreateDtme() {
		return createDtme;
	}
	public void setCreateDtme(Date createDtme) {
		this.createDtme = createDtme;
	}
	public Date getLastUpdtme() {
		return lastUpdtme;
	}
	public void setLastUpdtme(Date lastUpdtme) {
		this.lastUpdtme = lastUpdtme;
	}
	public long getCreateUserId() {
		return createUserId;
	}
	public void setCreateUserId(long createUserId) {
		this.createUserId = createUserId;
	}
	public long getLastUpdateUserId() {
		return lastUpdateUserId;
	}
	public void setLastUpdateUserId(long lastUpdateUserId) {
		this.lastUpdateUserId = lastUpdateUserId;
	}
	public String getDatasourceName() {
		return datasourceName;
	}
	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}
}
