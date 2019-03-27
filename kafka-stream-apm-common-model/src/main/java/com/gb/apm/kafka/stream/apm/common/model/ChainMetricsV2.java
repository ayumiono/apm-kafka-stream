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

public class ChainMetricsV2 implements Serializable {
	
	private static final long serialVersionUID = -1835254013477613713L;
	
	private static final Counter errorCounter = MetricsReporter.register("chainMetricsErrorCounter", new Counter());
	
	public static final int TIME_OUT = 1000;
	
	private Map<TimeWindow, TimeBucket> timeBuckets = new HashMap<>();
	
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
	
	public static ChainMetricsV2 timebucket(long timeBucketSize) {
		ChainMetricsV2 o = new ChainMetricsV2();
		o.timeBucketWindow = TimeWindows.of(timeBucketSize).advanceBy(timeBucketSize);
		return o;
	}
	
	private int invokeCount;
	private int timeoutCount;
	private int successCount;
	private int failCount;
	private long averageSpent;
	private long minSpent = Long.MAX_VALUE;
	private String minSpentTraceId;
	private long maxSpent;
	private String maxSpentTraceId;
	
	private int systemId;
	private long chainId;
	private String entranceMethodDesc;
	private String rpcStubMethodDesc;
	private int type;

	private long startMs;
	private long endMs;

	private Map<Integer, Integer> distributeChart = new HashMap<>();

	private Set<Integer> xAxis;
	private Collection<Integer> yAxis;

	public Collection<Integer> getyAxis() {
		return distributeChart.values();
	}

	public Set<Integer> getxAxis() {
		return distributeChart.keySet();
	}
	
	public ChainMetricsV2 hit(InvokeStack stack) {
		try {
			long spent = stack.getSpent();
			distribute(spent);
			if(spent > TIME_OUT) {
				this.timeoutCount ++;
			}
			Map<Long, TimeWindow> windows = this.timeBucketWindow.windowsFor(stack.getFinishTime());
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
				maxSpentTraceId = stack.getTraceId();
			}
			
			if(this.minSpent > spent) {
				minSpent = spent;
				minSpentTraceId = stack.getTraceId();
			}
			
			if (stack.getErrorFlag()) {
				failCount++;
			} else {
				successCount++;;
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
		// 2位则除10，四舍五入取整后再乘10，3位则除100，以此类推
		double factor = Math.pow(10, String.valueOf(spent).length() - 1);
		BigDecimal s = new BigDecimal(spent);
		BigDecimal f = new BigDecimal(factor);
		int result = s.divide(f).setScale(0, BigDecimal.ROUND_HALF_EVEN).multiply(f).intValue();
		if (distributeChart.containsKey(result)) {
			distributeChart.put(result, distributeChart.get(result) + 1);
		} else {
			distributeChart.put(result, 1);
		}
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

	public int getInvokeCount() {
		return invokeCount;
	}

	public void setInvokeCount(int invokeCount) {
		this.invokeCount = invokeCount;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public int getTimeoutCount() {
		return timeoutCount;
	}

	public void setTimeoutCount(int timeoutCount) {
		this.timeoutCount = timeoutCount;
	}

	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	public int getFailCount() {
		return failCount;
	}

	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}

	public long getAverageSpent() {
		return averageSpent;
	}

	public void setAverageSpent(long averageSpent) {
		this.averageSpent = averageSpent;
	}

	public long getMinSpent() {
		return minSpent;
	}

	public void setMinSpent(long minSpent) {
		this.minSpent = minSpent;
	}

	public String getMinSpentTraceId() {
		return minSpentTraceId;
	}

	public void setMinSpentTraceId(String minSpentTraceId) {
		this.minSpentTraceId = minSpentTraceId;
	}

	public long getMaxSpent() {
		return maxSpent;
	}

	public void setMaxSpent(long maxSpent) {
		this.maxSpent = maxSpent;
	}

	public String getMaxSpentTraceId() {
		return maxSpentTraceId;
	}

	public void setMaxSpentTraceId(String maxSpentTraceId) {
		this.maxSpentTraceId = maxSpentTraceId;
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

	public long getChainId() {
		return chainId;
	}

	public void setChainId(long chainId) {
		this.chainId = chainId;
	}

	public Map<Integer, Integer> getDistributeChart() {
		return distributeChart;
	}

	public void setDistributeChart(Map<Integer, Integer> distributeChart) {
		this.distributeChart = distributeChart;
	}

	public int getSystemId() {
		return systemId;
	}

	public void setSystemId(int systemId) {
		this.systemId = systemId;
	}
	
	public boolean equals(ChainMetricsV2 that) {
		return String.valueOf(this.chainId).equals(String.valueOf(that.chainId));
	}

	@Override
	public String toString() {
		return "ChainMetricsV2 [timeBuckets=" + timeBuckets + ", timeBucketWindow=" + timeBucketWindow
				+ ", invokeCount=" + invokeCount + ", successCount=" + successCount + ", failCount=" + failCount
				+ ", averageSpent=" + averageSpent + ", minSpent=" + minSpent + ", minSpentTraceId=" + minSpentTraceId
				+ ", maxSpent=" + maxSpent + ", maxSpentTraceId=" + maxSpentTraceId + ", systemId=" + systemId
				+ ", chainId=" + chainId + ", entranceMethodDesc=" + entranceMethodDesc + ", rpcStubMethodDesc="
				+ rpcStubMethodDesc + ", type=" + type + ", startMs=" + startMs + ", endMs=" + endMs + "]";
	}
}
