package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;
import java.util.List;

public class PageResult<T> implements Serializable {
	private static final long serialVersionUID = -4031250670115131570L;
	List<T> rs;
	Integer pageSize;
	Integer pageNo;
	
	int totalCount;
	
	public PageResult(int pageNo,int pageSize) {
		this.pageNo = pageNo;
		this.pageSize = pageSize;
	}
	
	public void setRs(List<T> rs) {
		this.rs = rs;
	}
	
	public List<T> getRs(){
		return rs;
	}
	
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}
	
	public int getTotalCount() {
		return this.totalCount;
	}
	

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public Integer getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}
}
