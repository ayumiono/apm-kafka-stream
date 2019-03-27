package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;

/**
 * 统一的需要统计的抽像实体
 * hit统计方法
 * timebuckets属性，时间桶统计
 * metrics汇总统计
 * @author xuelong.chen
 * @param <T> 统计事件
 * @param <M>	汇总统计指标类型
 * @param <TM>	时间桶统计指标类型
 */
public abstract class AbstractMetricObject<T,M extends AbstractCommonMetrics<T>, TM extends AbstractCommonMetrics<T>> implements Serializable{
	
	private static final long serialVersionUID = -3556370239668210258L;
	
	protected final M metrics;
	private final long startMs;
	private final long endMs;
	
	private Map<TimeWindow, TimeBucket<T, TM>> timeBuckets = new HashMap<>();
	private final TimeWindows timeBucketWindow;
	
	public AbstractMetricObject(long startMs, long endMs, M metrics, TimeWindows timeWindows) {
		this.startMs = startMs;
		this.endMs = endMs;
		this.metrics = metrics;
		this.timeBucketWindow = timeWindows;
	}
	
	public AbstractMetricObject(long startMs, long endMs) {
		this.metrics = initMetrics(startMs);
		this.timeBucketWindow = initTimeWindows();
		this.startMs = startMs;
		this.endMs = endMs;
	}
	
	public long getStartMs() {
		return startMs;
	}

	public long getEndMs() {
		return endMs;
	}

	public M getMetrics() {
		return metrics;
	}
	
	public Map<TimeWindow, TimeBucket<T, TM>> getTimeBuckets(){
		return timeBuckets;
	}
	public void setTimeBuckets(Map<TimeWindow, TimeBucket<T, TM>> timeBuckets) {
		this.timeBuckets = timeBuckets;
	}
	
	public TimeWindows getTimeBucketWindow() {
		return timeBucketWindow;
	}
	
	/**
	 * 返回指定时间范围内的所有时间桶
	 * @param startms
	 * @param endms
	 * @return
	 */
	public List<TimeBucket<T,TM>> buckets(long startms,long endms) {
		List<TimeBucket<T,TM>> result = new ArrayList<>();
		timeBuckets.entrySet().forEach(new Consumer<Entry<TimeWindow, TimeBucket<T,TM>>>() {
			@Override
			public void accept(Entry<TimeWindow, TimeBucket<T,TM>> t) {
				if(t.getKey().start() >= startms && t.getKey().end() <= endms) {
					result.add(t.getValue());
				}
			}
		});
		return result;
	}
	
	/**
	 * 返回指定时间范围内的所有时间桶,根据时间范围自动合并
	 * @param startms
	 * @param endms
	 * @return
	 */
	public List<TimeBucket<T,TM>> buckets_auto_merge(long startms, long endms) {
		long interval = (endms - startms) / 60;//均分成60分
		if(interval <= 5*60*1000) 
			return this.buckets(startms, endms);
		Map<TimeWindow, TimeBucket<T, TM>> tmp = new HashMap<>();
		TimeWindows timewindows = TimeWindows.of(interval).advanceBy(interval);
		timeBuckets.entrySet().forEach(new Consumer<Entry<TimeWindow, TimeBucket<T,TM>>>() {
			@Override
			public void accept(Entry<TimeWindow, TimeBucket<T,TM>> t) {
				if(t.getKey().start() >= startms && t.getKey().end() <= endms) {
					Map<Long, TimeWindow> windows = timewindows.windowsFor(t.getValue().getStartMs());
					for (Entry<Long, TimeWindow> entry : windows.entrySet()) {
						TimeWindow window = entry.getValue();
						if (tmp.get(window) == null) {
							tmp.put(window, t.getValue());
						}else {
							tmp.get(window).getMetrics().aggregate(t.getValue().getMetrics());
						}
					}
				}
			}
		});
		return Arrays.asList(tmp.values().toArray(new TimeBucket[] {})); 
	}
	
	public AbstractMetricObject<T, M, TM> hit(T t, long time) {
		this.metrics.hit(t);
		Map<Long, TimeWindow> windows = this.timeBucketWindow.windowsFor(time);
		for (Entry<Long, TimeWindow> entry : windows.entrySet()) {
			long startms = entry.getKey();
			TimeWindow window = entry.getValue();
			if (timeBuckets.get(window) == null) {
				TimeBucket<T, TM> timeBucket = new TimeBucket<>(initTimeBucketMetrics(startms),startms);
				timeBuckets.put(window, timeBucket);
			}
			timeBuckets.get(window).hit(t);
		}
		doCustomLogic(t);
		return this;
	}
	
	/**
	 * 统计指标完成后,做一些子类自定义的业务处理
	 * @param t
	 */
	public abstract void doCustomLogic(T t);
	
	public abstract M initMetrics(long startMs);
	
	public abstract TM initTimeBucketMetrics(long startMs);
	
	public abstract TimeWindows initTimeWindows();
}
