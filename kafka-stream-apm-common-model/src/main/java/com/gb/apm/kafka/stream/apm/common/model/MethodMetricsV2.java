package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;

import com.codahale.metrics.Counter;
import com.gb.apm.kafka.stream.apm.common.model.utils.MetricsReporter;

public class MethodMetricsV2 implements Serializable {
	
	private Map<TimeWindow, TimeBucket> timeBuckets = new HashMap<>();
	
	private static final Counter errorCounter = MetricsReporter.register("chainMetricsErrorCounter", new Counter());
	
	public Map<TimeWindow, TimeBucket> getTimeBuckets(){
		return timeBuckets;
	}
	
	public List<TimeBucket> buckets(){
		List<TimeBucket> result = new ArrayList<>();
		timeBuckets.entrySet().forEach(new Consumer<Entry<TimeWindow, TimeBucket>>() {
			@Override
			public void accept(Entry<TimeWindow, TimeBucket> t) {
				result.add(t.getValue());
			}
		});
		return result;
	}
	
	public List<TimeBucket> buckets(long startms,long endms) {
		List<TimeBucket> result = new ArrayList<>();
		timeBuckets.entrySet().forEach(new Consumer<Entry<TimeWindow, TimeBucket>>() {
			@Override
			public void accept(Entry<TimeWindow, TimeBucket> t) {
				if(t.getKey().start()>=startms && t.getKey().end()<=endms) {
					result.add(t.getValue());
				}
			}
		});
		return result;
	}
	
	private TimeWindows timeBucketWindow;
	
	public static MethodMetricsV2 timebucket(long timeBucketSize) {
		MethodMetricsV2 o = new MethodMetricsV2();
		o.timeBucketWindow = TimeWindows.of(timeBucketSize).advanceBy(timeBucketSize);
		return o;
	}
	
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
	
	
	private int systemId;
	private long chainId;
	private long id;
	
	public Collection<Integer> getyAxis() {
		return distributeChart.values();
	}

	public Set<Integer> getxAxis() {
		return distributeChart.keySet();
	}
	
	public MethodMetricsV2 hit(StackFrame frame) {
		try {
			long spent = frame.getPopTimeStamp() - frame.getPushTimeStamp();
			distribute(spent);
			
			Map<Long, TimeWindow> windows = this.timeBucketWindow.windowsFor(frame.getPushTimeStamp());
			for(Entry<Long, TimeWindow> entry : windows.entrySet()) {
				long startms = entry.getKey();
				TimeWindow window = entry.getValue();
				if(timeBuckets.get(window) == null) {
					TimeBucket timeBucket = new TimeBucket();
					timeBucket.startMs = startms;
					timeBuckets.put(window, timeBucket);
				}
				timeBuckets.get(window).hit(spent);
			}
			if(this.maxSpent == 0 || this.maxSpent < spent) {
				maxSpent = spent;
				maxRelativeTraceId = frame.getTraceId();
			}
			
			if(this.minSpent > spent) {
				minSpent = spent;
				minRelativeTraceId = frame.getTraceId();
			}
			this.averageSpent = (this.averageSpent*invokeCount+spent)/(this.invokeCount+1);
			this.invokeCount ++;
		} catch (Exception e) {
			errorCounter.inc();
			e.printStackTrace();
		}
		return this;
	}

	private void distribute(long spent) {
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
	
	public TimeWindows getTimeBucketWindow() {
		return timeBucketWindow;
	}

	public void setTimeBucketWindow(TimeWindows timeBucketWindow) {
		this.timeBucketWindow = timeBucketWindow;
	}

	public void setTimeBuckets(Map<TimeWindow, TimeBucket> timeBuckets) {
		this.timeBuckets = timeBuckets;
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

	public int getSystemId() {
		return systemId;
	}

	public void setSystemId(int systemId) {
		this.systemId = systemId;
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

	@Override
	public String toString() {
		return "MethodMetricsV2 [timeBuckets=" + timeBuckets + ", timeBucketWindow=" + timeBucketWindow + ", systemId="
				+ systemId + ", chainId=" + chainId + ", id=" + id + ", methodDesc=" + methodDesc + ", className="
				+ className + ", methodName=" + methodName + ", startMs=" + startMs + ", endMs=" + endMs
				+ ", invokeCount=" + invokeCount + ", averageSpent=" + averageSpent + ", maxSpent=" + maxSpent
				+ ", maxRelativeTraceId=" + maxRelativeTraceId + ", minSpent=" + minSpent + ", minRelativeTraceId="
				+ minRelativeTraceId + ", successCount=" + successCount + ", errorCount=" + errorCount
				+ ", exceptionCount=" + exceptionCount + "]";
	}
}
