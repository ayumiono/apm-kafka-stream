package com.gb.apm.kafka.stream.apm.common.grafana;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gb.apm.kafka.stream.apm.common.model.CodeMetrics;
import com.gb.apm.kafka.stream.apm.common.model.JVMGCMetrics;
import com.gb.apm.kafka.stream.apm.common.model.JVMMetrics;
import com.gb.apm.kafka.stream.apm.common.model.TimeBucket;

/**
 * grafana 数据模型
 * 
 * @author xuelong.chen
 *
 */
public class TimeSeriesData implements Serializable {
	private static final long serialVersionUID = -5206991042376717934L;

	private String target;
	private List<Object[]> datapoints = new ArrayList<>();

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public List<Object[]> getDatapoints() {
		return datapoints;
	}

	public void setDatapoints(List<Object[]> datapoints) {
		this.datapoints = datapoints;
	}

	public void addDatapoint(Object[] point) {
		if (point == null || point.length != 2) {
			throw new IllegalStateException("datapoint cannot be null and must be a two diamention arr");
		}
		if (!(point[1] instanceof Long)) {
			throw new IllegalStateException("datapoint must hava a timestamp data");
		}

		datapoints.add(point);
	}
	
	public static List<TimeSeriesData> wrapCodeMetrics(List<CodeMetrics> metrics) {
		List<TimeSeriesData> list = new ArrayList<>();
		Map<String, Long> aggration = new HashMap<>();
		for(CodeMetrics m : metrics) {
			for(Entry<Long, Long> code : m.getCounts().entrySet()) {
				if(!aggration.containsKey(code.getKey().toString())) {
					aggration.put(code.getKey().toString(), 0L);
				}
				aggration.put(code.getKey().toString(), aggration.get(code.getKey().toString()) + code.getValue());
			}
		}
		for(Entry<String, Long> code : aggration.entrySet()) {
			TimeSeriesData series = new TimeSeriesData();
			series.setTarget(code.getKey().toString());
			series.addDatapoint(new Object[] {code.getValue(),System.currentTimeMillis()});
			list.add(series);
		}
		return list;
	}
	
	public static List<TimeSeriesData> wrapCodeMetrics(CodeMetrics metrics) {
		List<TimeSeriesData> list = new ArrayList<>();
		for(Entry<Long, Long> code : metrics.getCounts().entrySet()) {
			TimeSeriesData series = new TimeSeriesData();
			series.setTarget(code.getKey().toString());
			series.addDatapoint(new Object[] {code.getValue(),System.currentTimeMillis()});
			list.add(series);
		}
		return list;
	}
	
	public static List<TimeSeriesData> wrapSingleState(String target,Object stat) {
		TimeSeriesData singleState = new TimeSeriesData();
		singleState.setTarget(target);
		singleState.addDatapoint(new Object[] {stat,System.currentTimeMillis()});
		return Collections.singletonList(singleState);
	}

	public static List<TimeSeriesData> wrapTimebucketData(List<TimeBucket> originalData) {
		Collections.sort(originalData);
		List<TimeSeriesData> series = new ArrayList<>();
		TimeSeriesData average_series = new TimeSeriesData();
		average_series.setTarget("average_time_spent");
		TimeSeriesData count_series = new TimeSeriesData();
		count_series.setTarget("invoke_count");
		for (TimeBucket bucket : originalData) {
			average_series.addDatapoint(new Long[] { bucket.getAverageSpent(), bucket.getStartMs() });
			count_series.addDatapoint(new Long[] { bucket.getInvokeCount(), bucket.getStartMs() });
		}
		series.add(average_series);
		series.add(count_series);
		return series;
	}

	public static List<TimeSeriesData> wrapCpuLoad(List<JVMMetrics> metrics) {
		TimeSeriesData cpuload = new TimeSeriesData();
		cpuload.setTarget("jvm process cpu load");
		for(JVMMetrics metric : metrics) {
			cpuload.addDatapoint(new Object[] {metric.getProcessCpuLoad(),metric.getStartMs()});
		}
		return Collections.singletonList(cpuload);
	}
	
	public static List<TimeSeriesData> wrapEdenMemory(List<JVMMetrics> metrics) {
		List<TimeSeriesData> series = new ArrayList<>();
		TimeSeriesData used = new TimeSeriesData();
		TimeSeriesData usedRatio = new TimeSeriesData();
		used.setTarget("PS-Eden-Space-Used");
		usedRatio.setTarget("PS-Eden-Space-Used-Ratio");
		for(JVMMetrics metric : metrics) {
			if(metric.getMemoryPool() == null) continue;
			used.addDatapoint(new Object[] {metric.getMemoryPool().get("jvm.pools.PS-Eden-Space").getMemoryUsage().getUsed(),metric.getStartMs()});
			usedRatio.addDatapoint(new Object[] {metric.getMemoryPool().get("jvm.pools.PS-Eden-Space").getUsageRatio(),metric.getStartMs()});
		}
		series.add(used);
		series.add(usedRatio);
		return series;
	}
	
	public static List<TimeSeriesData> wrapSupviorMemory(List<JVMMetrics> metrics) {
		List<TimeSeriesData> series = new ArrayList<>();
		TimeSeriesData used = new TimeSeriesData();
		TimeSeriesData usedRatio = new TimeSeriesData();
		used.setTarget("PS-Survivor-Space-Used");
		usedRatio.setTarget("PS-Survivor-Space-Used-Ratio");
		for(JVMMetrics metric : metrics) {
			if(metric.getMemoryPool() == null) continue;
			used.addDatapoint(new Object[] {metric.getMemoryPool().get("jvm.pools.PS-Survivor-Space").getMemoryUsage().getUsed(),metric.getStartMs()});
			usedRatio.addDatapoint(new Object[] {metric.getMemoryPool().get("jvm.pools.PS-Survivor-Space").getUsageRatio(),metric.getStartMs()});
		}
		series.add(used);
		series.add(usedRatio);
		return series;
	}
	
	public static List<TimeSeriesData> wrapOldMemory(List<JVMMetrics> metrics) {
		List<TimeSeriesData> series = new ArrayList<>();
		TimeSeriesData used = new TimeSeriesData();
		TimeSeriesData usedRatio = new TimeSeriesData();
		used.setTarget("PS-Old-Gen-Used");
		usedRatio.setTarget("PS-Old-Gen-Used-Ratio");
		for(JVMMetrics metric : metrics) {
			if(metric.getMemoryPool() == null) continue;
			used.addDatapoint(new Object[] {metric.getMemoryPool().get("jvm.pools.PS-Old-Gen").getMemoryUsage().getUsed(),metric.getStartMs()});
			usedRatio.addDatapoint(new Object[] {metric.getMemoryPool().get("jvm.pools.PS-Old-Gen").getUsageRatio(),metric.getStartMs()});
		}
		series.add(used);
		series.add(usedRatio);
		return series;
	}
	
	public static List<TimeSeriesData> wrapHeapMemory(List<JVMMetrics> metrics) {
		List<TimeSeriesData> series = new ArrayList<>();
		TimeSeriesData used = new TimeSeriesData();
		TimeSeriesData usedRatio = new TimeSeriesData();
		used.setTarget("Heap-Memory-Used");
		usedRatio.setTarget("Heap-Memory-Used-Ratio");
		for(JVMMetrics metric : metrics) {
			if(metric.getHeapMemory() == null) continue;
			used.addDatapoint(new Object[] {metric.getHeapMemory().getMemoryUsage().getUsed(),metric.getStartMs()});
			usedRatio.addDatapoint(new Object[] {metric.getHeapMemory().getUsageRatio(),metric.getStartMs()});
		}
		series.add(used);
		series.add(usedRatio);
		return series;
	}
	
	public static List<TimeSeriesData> wrapNonHeapMemory(List<JVMMetrics> metrics) {
		List<TimeSeriesData> series = new ArrayList<>();
		TimeSeriesData used = new TimeSeriesData();
		TimeSeriesData usedRatio = new TimeSeriesData();
		used.setTarget("Non-Heap-Memory-Used");
		usedRatio.setTarget("Non-Heap-Memory-Used-Ratio");
		for(JVMMetrics metric : metrics) {
			if(metric.getNonHeapMemory() == null) continue;
			used.addDatapoint(new Object[] {metric.getNonHeapMemory().getMemoryUsage().getUsed(),metric.getStartMs()});
			usedRatio.addDatapoint(new Object[] {metric.getNonHeapMemory().getUsageRatio(),metric.getStartMs()});
		}
		series.add(used);
		series.add(usedRatio);
		return series;
	}
	
	public static List<TimeSeriesData> wrapThread(List<JVMMetrics> metrics) {
		List<TimeSeriesData> series = new ArrayList<>();
		TimeSeriesData peakThreadCount = new TimeSeriesData();
		peakThreadCount.setTarget("thread-peak-count");
		TimeSeriesData threadCount = new TimeSeriesData();
		threadCount.setTarget("active-thread-count");
		for(JVMMetrics metric : metrics) {
			if(metric.getThread() == null) continue;
			peakThreadCount.addDatapoint(new Object[] {metric.getThread().getPeakThreadCount(),metric.getStartMs()});
			threadCount.addDatapoint(new Object[] {metric.getThread().getThreadCount(),metric.getStartMs()});
		}
		series.add(peakThreadCount);
		series.add(threadCount);
		return series;
	}
	
	public static List<TimeSeriesData> wrapGc(List<JVMMetrics> metrics) {
		Map<String, TimeSeriesData> m = new HashMap<>();
		for(JVMMetrics metric : metrics) {
			if(metric.getGc() == null) continue;
			for(JVMGCMetrics gcm : metric.getGc()) {
				String gccount = gcm.getGcAlgorithm()+"-collection-count";
				String gctime = gcm.getGcAlgorithm()+"-collection-time";
				if(m.containsKey(gccount)) {
					m.get(gccount).addDatapoint(new Object[] {gcm.getCollectionCount(),metric.getStartMs()});
				}else {
					TimeSeriesData s = new TimeSeriesData();
					s.setTarget(gccount);
					s.addDatapoint(new Object[] {gcm.getCollectionCount(),metric.getStartMs()});
					m.put(gccount, s);
				}
				if(m.containsKey(gctime)) {
					m.get(gctime).addDatapoint(new Object[] {gcm.getCollectionTime(),metric.getStartMs()});
				}else {
					TimeSeriesData s = new TimeSeriesData();
					s.setTarget(gctime);
					s.addDatapoint(new Object[] {gcm.getCollectionTime(),metric.getStartMs()});
					m.put(gctime, s);
				}
			}
		}
		return new ArrayList<>(m.values());
	}
}
