package com.gb.apm.kafka.stream.apm.common.model;

public class MemoryUsage {
	private long init;
	private long used;
	private long committed;
	private long max;

	public MemoryUsage() {
	}

	public MemoryUsage(long init, long used, long committed, long max) {
		if (init < -1) {
			throw new IllegalArgumentException("init parameter = " + init + " is negative but not -1.");
		}
		if (max < -1) {
			throw new IllegalArgumentException("max parameter = " + max + " is negative but not -1.");
		}
		if (used < 0) {
			throw new IllegalArgumentException("used parameter = " + used + " is negative.");
		}
		if (committed < 0) {
			throw new IllegalArgumentException("committed parameter = " + committed + " is negative.");
		}
		if (used > committed) {
			throw new IllegalArgumentException("used = " + used + " should be <= committed = " + committed);
		}
		if (max >= 0 && committed > max) {
			throw new IllegalArgumentException("committed = " + committed + " should be < max = " + max);
		}

		this.init = init;
		this.used = used;
		this.committed = committed;
		this.max = max;
	}

	public long getInit() {
		return init;
	}

	public void setInit(long init) {
		this.init = init;
	}

	public long getUsed() {
		return used;
	}

	public void setUsed(long used) {
		this.used = used;
	}

	public long getCommitted() {
		return committed;
	}

	public void setCommitted(long committed) {
		this.committed = committed;
	}

	public long getMax() {
		return max;
	}

	public void setMax(long max) {
		this.max = max;
	}
}
