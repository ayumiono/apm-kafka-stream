package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MethodMetrics implements Serializable {

	public static String POP_TIME_KEY = "popTimeStamp";
	public static String PUSH_TIME_KEY = "pushTimeStamp";
	public static String EXCEPTION_KEY = "exception";
	public static String ERROR_KEY = "error";
	public static String METHOD_DESC_KEY = "methodDesc";
	public static String TRACE_ID_KEY = "traceId";
	public static String METHOD_NAME_KEY = "methodName";

	private static final long serialVersionUID = 1756654770280920228L;

	private Map<Integer,Integer> distributeChart;
	
	private Set<Integer> xAxis;
	private Collection<Integer> yAxis;
	
	public Collection<Integer> getyAxis() {
		return distributeChart.values();
	}

	public Set<Integer> getxAxis() {
		return distributeChart.keySet();
	}

	public void distribute(long spent) {
		if(distributeChart == null) {
			distributeChart = new HashMap<>();
		}
		//2位则除10，四舍五入取整后再乘10，3位则除100，以此类推
		double factor = Math.pow(10, String.valueOf(spent).length()-1);
		BigDecimal s = new BigDecimal(spent);
		BigDecimal f = new BigDecimal(factor);
		int result = s.divide(f).setScale(0, BigDecimal.ROUND_HALF_EVEN).multiply(f).intValue();
		if(distributeChart.containsKey(result)) {
			distributeChart.put(result,distributeChart.get(result)+1);
		}else {
			distributeChart.put(result, 1);
		}
	}

	/**
	 * 方法描述
	 */
	private String methodDesc;
	
	private String className;
	
	private String methodName;
	
	/**
	 * 时间窗口开始时间
	 */
	private long startMs;
	/**
	 * 时间窗口结束时间
	 */
	private long endMs;

	/**
	 * 调用次数
	 */
	private long invokeCount;
	/**
	 * 平均耗时
	 */
	private long averageSpent;

	/**
	 * 最长耗时
	 */
	private long maxSpent;
	/**
	 * 最长耗时相对应链路ID
	 */
	private String maxRelativeTraceId;

	/**
	 * 最短耗时
	 */
	private long minSpent = Long.MAX_VALUE;
	/**
	 * 最短耗时相对应链路ID
	 */
	private String minRelativeTraceId;

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

	public String getMethodDesc() {
		return methodDesc;
	}

	public void setMethodDesc(String methodDesc) {
		this.methodDesc = methodDesc;
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

	public Map<Integer, Integer> getDistributeChart() {
		return distributeChart;
	}

	public void setDistributeChart(Map<Integer, Integer> distributeChart) {
		this.distributeChart = distributeChart;
	}

	public long getMaxSpent() {
		return maxSpent;
	}

	public void setMaxSpent(long maxSpent) {
		this.maxSpent = maxSpent;
	}

	public String getMaxRelativeTraceId() {
		return maxRelativeTraceId;
	}

	public void setMaxRelativeTraceId(String maxRelativeTraceId) {
		this.maxRelativeTraceId = maxRelativeTraceId;
	}

	public long getMinSpent() {
		return minSpent;
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

	public void setMinSpent(long minSpent) {
		this.minSpent = minSpent;
	}

	public String getMinRelativeTraceId() {
		return minRelativeTraceId;
	}

	public void setMinRelativeTraceId(String minRelativeTraceId) {
		this.minRelativeTraceId = minRelativeTraceId;
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
}
