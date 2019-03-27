package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;

import com.gb.apm.model.TSpan;

public class TimeBucket implements Serializable,Comparable<TimeBucket>{
	
	private static final long serialVersionUID = -5581515340918388574L;
	
	public long invokeCount;
	public long minSpent;
	public long maxSpent;
	public long averageSpent;
	public long startMs;
	
	public long getInvokeCount() {
		return invokeCount;
	}

	public long getMinSpent() {
		return minSpent;
	}

	public long getMaxSpent() {
		return maxSpent;
	}

	public long getAverageSpent() {
		return averageSpent;
	}

	public long getStartMs() {
		return startMs;
	}

	public void setInvokeCount(int invokeCount) {
		this.invokeCount = invokeCount;
	}

	public void setMinSpent(long minSpent) {
		this.minSpent = minSpent;
	}

	public void setMaxSpent(long maxSpent) {
		this.maxSpent = maxSpent;
	}

	public void setAverageSpent(long averageSpent) {
		this.averageSpent = averageSpent;
	}

	public void setStartMs(long startMs) {
		this.startMs = startMs;
	}
	
	public void hit(long spent) {
		if(minSpent == 0 || minSpent > spent) {
			minSpent = spent;
		}
		if(maxSpent == 0 || maxSpent < spent) {
			maxSpent = spent;
		}
		this.averageSpent = (this.averageSpent * invokeCount + spent)/(this.invokeCount + 1);
		this.invokeCount++;
	}

	public void hit(TSpan tspan) {
		long spent = tspan.getElapsed();
		this.hit(spent);
	}
	
	@Override
	public String toString() {
		return "TimeBucket("+startMs+") [invokeCount=" + invokeCount + ", minSpent=" + minSpent + ", maxSpent=" + maxSpent
				+ ", averageSpent=" + averageSpent + "]";
	}

	@Override
	public int compareTo(TimeBucket o) {
		if(this.startMs<o.startMs) return -1;
		if(this.startMs>o.startMs) return 1;
		return 0;
	}

	public void setInvokeCount(long invokeCount) {
		this.invokeCount = invokeCount;
	}
}
