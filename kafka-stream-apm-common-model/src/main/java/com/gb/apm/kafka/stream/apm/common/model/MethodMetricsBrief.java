package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;

/**
 * 方法统计概要信息,只用来前端列表展示
 * @author xuelong.chen
 *
 */
public class MethodMetricsBrief implements Serializable {
	private static final long serialVersionUID = -5525545201055450133L;
	
	private long methodId;

	/**
	 * 方法描述
	 */
	private String methodDesc;
	
	private String className;
	
	private String methodName;
	
	/**
	 * 调用次数
	 */
	private long invokeCount;
	/**
	 * 平均耗时
	 */
	private long averageSpent;
	
	/**
	 * 成功次数
	 */
	private long successCount;

	/**
	 * 异常次数
	 */
	private long errorCount;

	/**
	 * 失败次数
	 */
	private long exceptionCount;

	public long getMethodId() {
		return methodId;
	}

	public void setMethodId(long methodId) {
		this.methodId = methodId;
	}

	public String getMethodDesc() {
		return methodDesc;
	}

	public void setMethodDesc(String methodDesc) {
		this.methodDesc = methodDesc;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public long getInvokeCount() {
		return invokeCount;
	}

	public void setInvokeCount(long invokeCount) {
		this.invokeCount = invokeCount;
	}

	public long getAverageSpent() {
		return averageSpent;
	}

	public void setAverageSpent(long averageSpent) {
		this.averageSpent = averageSpent;
	}

	public long getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(long successCount) {
		this.successCount = successCount;
	}

	public long getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(long errorCount) {
		this.errorCount = errorCount;
	}

	public long getExceptionCount() {
		return exceptionCount;
	}

	public void setExceptionCount(long exceptionCount) {
		this.exceptionCount = exceptionCount;
	}
	
	public static MethodMetricsBrief brief(MethodMetrics v) {
		MethodMetricsBrief brief = new MethodMetricsBrief();
		brief.setAverageSpent(v.getAverageSpent());
		brief.setClassName(v.getClassName());
		brief.setErrorCount(v.getErrorCount());
		brief.setExceptionCount(v.getExceptionCount());
		brief.setInvokeCount(v.getInvokeCount());
		brief.setMethodDesc(v.getMethodDesc());
		brief.setMethodName(v.getMethodName());
		brief.setSuccessCount(v.getSuccessCount());
		return brief;
	}
	
	public static MethodMetricsBrief brief(MethodMetricsV2 v) {
		MethodMetricsBrief brief = new MethodMetricsBrief();
		brief.setAverageSpent(v.getAverageSpent());
		brief.setClassName(v.getClassName());
		brief.setErrorCount(v.getErrorCount());
		brief.setExceptionCount(v.getExceptionCount());
		brief.setInvokeCount(v.getInvokeCount());
		brief.setMethodDesc(v.getMethodDesc());
		brief.setMethodName(v.getMethodName());
		brief.setSuccessCount(v.getSuccessCount());
		return brief;
	}
}
