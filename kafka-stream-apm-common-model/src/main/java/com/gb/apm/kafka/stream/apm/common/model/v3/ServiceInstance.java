package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;

import com.gb.apm.kafka.stream.apm.common.model.TimeBucket;
import com.gb.apm.model.Annotation;
import com.gb.apm.model.TSpan;

/**
 * 应用实例
 * provider side
 * @author xuelong.chen
 *
 */
@Deprecated
public class ServiceInstance {

	private String endpoint;// host:port

	private Service service;
	
	private List<Annotation> annotations;

	private Map<TimeWindow, TimeBucket> timeBuckets = new HashMap<>();
	
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
	
	private int invokeCount;
	private int successCount;
	private int failCount;
	private long averageSpent;
	private long minSpent = Long.MAX_VALUE;
	private String minSpentTraceId;
	private long maxSpent = Long.MIN_VALUE;
	private String maxSpentTraceId;

	private long startMs;
	private long endMs;
	

	public ServiceInstance() {
	}

	public ServiceInstance(Service service, String endpoint) {
		this.service = service;
		this.endpoint = endpoint;
	}

	public static ServiceInstance timebucket(Service application, String endpoint, List<Annotation> annotations, long timeBucketSize) {
		ServiceInstance instance = new ServiceInstance(application, endpoint);
		instance.setAnnotations(annotations);
		instance.setTimeBucketWindow(TimeWindows.of(timeBucketSize).advanceBy(timeBucketSize));
		return instance;
	}

	public ServiceInstance hit(TSpan frame) {
		try {
			long spent = frame.getElapsed();
			boolean isErr = frame.getErr() != null;
			String transactionId = frame.getTransactionId();
			Map<Long, TimeWindow> windows = this.timeBucketWindow.windowsFor(frame.getStartTime());
			for (Entry<Long, TimeWindow> entry : windows.entrySet()) {
				long startms = entry.getKey();
				TimeWindow window = entry.getValue();
				if (timeBuckets.get(window) == null) {
					TimeBucket timeBucket = new TimeBucket();
					timeBucket.startMs = startms;
					timeBuckets.put(window, timeBucket);
				}
				timeBuckets.get(window).hit(frame);
			}
			if(this.maxSpent == 0 || this.maxSpent < spent) {
				maxSpent = spent;
				maxSpentTraceId = transactionId;
			}
			
			if(this.minSpent > spent) {
				minSpent = spent;
				minSpentTraceId = transactionId;
			}
			
			if (isErr) {
				failCount++;
			} else {
				successCount++;;
			}
			this.averageSpent = (this.averageSpent*invokeCount+spent)/(this.invokeCount+1);
			this.invokeCount ++;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public Map<TimeWindow, TimeBucket> getTimeBuckets() {
		return timeBuckets;
	}

	public void setTimeBuckets(Map<TimeWindow, TimeBucket> timeBuckets) {
		this.timeBuckets = timeBuckets;
	}

	public TimeWindows getTimeBucketWindow() {
		return timeBucketWindow;
	}

	public void setTimeBucketWindow(TimeWindows timeBucketWindow) {
		this.timeBucketWindow = timeBucketWindow;
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

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<Annotation> annotations) {
		this.annotations = annotations;
	}
}
