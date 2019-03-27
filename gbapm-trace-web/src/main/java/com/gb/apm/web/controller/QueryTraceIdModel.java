package com.gb.apm.web.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class QueryTraceIdModel {
	private Date[] time_range;
	private int agentId;
	private List<Condition> conditions;
	private String traceId;
	
	private int pageSize;
	private int pageIndex;

	public Date[] getTime_range() {
		return time_range;
	}

	public void setTime_range(Date[] time_range) {
		this.time_range = time_range;
	}

	public int getAgentId() {
		return agentId;
	}

	public void setAgentId(int agentId) {
		this.agentId = agentId;
	}

	public List<Condition> getConditions() {
		return conditions;
	}

	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}
	
	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}

	public static class Condition {
		private int serviceType;
		private String args;
		private String result;
		private String api;
		public int getServiceType() {
			return serviceType;
		}
		public void setServiceType(int serviceType) {
			this.serviceType = serviceType;
		}
		public String getArgs() {
			return args;
		}
		public void setArgs(String args) {
			this.args = args;
		}
		public String getResult() {
			return result;
		}
		public void setResult(String result) {
			this.result = result;
		}

		public String getApi() {
			return api;
		}
		public void setApi(String api) {
			this.api = api;
		}
		@Override
		public String toString() {
			return "Condition [serviceType=" + serviceType + ", args=" + args + ", result=" + result + ", api=" + api + "]";
		}
	}

	@Override
	public String toString() {
		return "QueryTraceIdModel [time_range=" + Arrays.toString(time_range) + ", agentId=" + agentId + ", conditions="
				+ conditions + ", traceId=" + traceId + "]";
	}
}
