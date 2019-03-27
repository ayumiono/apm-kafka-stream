package com.gb.apm.kafka.stream.apm.common.model;

/**
 * @author emeroad
 * @author netspider
 */
public class Histogram {
	private long fastCount;
	private long normalCount;
	private long slowCount;
	private long verySlowCount;
	private long errorCount; // for backward compatibility.
	private long fastErrorCount;
	private long normalErrorCount;
	private long slowErrorCount;
	private long verySlowErrorCount;

	public long getFastCount() {
		return fastCount;
	}

	public void setFastCount(long fastCount) {
		this.fastCount = fastCount;
	}

	public long getNormalCount() {
		return normalCount;
	}

	public void setNormalCount(long normalCount) {
		this.normalCount = normalCount;
	}

	public long getSlowCount() {
		return slowCount;
	}

	public void setSlowCount(long slowCount) {
		this.slowCount = slowCount;
	}

	public long getVerySlowCount() {
		return verySlowCount;
	}

	public void setVerySlowCount(long verySlowCount) {
		this.verySlowCount = verySlowCount;
	}

	public long getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(long errorCount) {
		this.errorCount = errorCount;
	}

	public long getFastErrorCount() {
		return fastErrorCount;
	}

	public void setFastErrorCount(long fastErrorCount) {
		this.fastErrorCount = fastErrorCount;
	}

	public long getNormalErrorCount() {
		return normalErrorCount;
	}

	public void setNormalErrorCount(long normalErrorCount) {
		this.normalErrorCount = normalErrorCount;
	}

	public long getSlowErrorCount() {
		return slowErrorCount;
	}

	public void setSlowErrorCount(long slowErrorCount) {
		this.slowErrorCount = slowErrorCount;
	}

	public long getVerySlowErrorCount() {
		return verySlowErrorCount;
	}

	public void setVerySlowErrorCount(long verySlowErrorCount) {
		this.verySlowErrorCount = verySlowErrorCount;
	}

}