package com.gb.apm.kafka.stream.apm.common.model.v3;

public class TimeBucket<T, M extends AbstractCommonMetrics<T>> implements Comparable<TimeBucket<T, M>> {
	
	public final M metrics;
	public final long startMs;
	
	public TimeBucket(M metrics, long startMs) {
		this.metrics = metrics;
		this.startMs = startMs;
	}

	@Override
	public int compareTo(TimeBucket<T, M> o) {
		if (this.startMs < o.startMs)
			return -1;
		if (this.startMs > o.startMs)
			return 1;
		return 0;
	}
	
	public M getMetrics() {
		return metrics;
	}

	public long getStartMs() {
		return startMs;
	}
	
	public void hit(T t) {
		metrics.hit(t);
	};
}
