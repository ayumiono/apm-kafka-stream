package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.io.Serializable;

/**
 * 指标抽像类
 * 
 * @author xuelong.chen
 *
 * @param <T>
 */
public abstract class AbstractCommonMetrics<T> implements Serializable,Comparable<AbstractCommonMetrics<T>> {
	
	private static final long serialVersionUID = -5581515340918388574L;
	
	private long startMs;
	
	public long getStartMs() {
		return startMs;
	}

	public void setStartMs(long startMs) {
		this.startMs = startMs;
	}
	
	public abstract void hit(T t);
	
	public abstract void aggregate(AbstractCommonMetrics<T> that);

	@Override
	public int compareTo(AbstractCommonMetrics<T> o) {
		return this.startMs > o.startMs ? 1 : -1;
	}
}
