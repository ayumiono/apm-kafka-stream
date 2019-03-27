package com.gb.apm.kafka.stream.apm.common.model.v3;

public class TraceSnapshot implements Comparable<TraceSnapshot>{
	private String transactionId;
	private long startTime;
	private boolean error;
	private int elapsed;

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public int getElapsed() {
		return elapsed;
	}

	public void setElapsed(int elapsed) {
		this.elapsed = elapsed;
	}

	/* 
	 * 展示优先级
	 * error > elapsed > starttimestamp
	 */
	@Override
	public int compareTo(TraceSnapshot o) {
		if(this.error == o.error) {
			if(this.elapsed == o.elapsed) {
				if(this.startTime == o.elapsed) {
					return 0;
				}else {
					return this.startTime > o.startTime ? 1 : -1;
				}
			}else {
				return this.elapsed - o.elapsed;
			}
		}else {
			return this.error ? 1 : -1;
		}
	}
}
