package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;

public class JvmMemoryUsage implements Serializable{
	private static final long serialVersionUID = 3172661439962689004L;
	private MemoryUsage memoryUsage;
	private double usageRatio;
	
	public JvmMemoryUsage() {}
	
	public MemoryUsage getMemoryUsage() {
		return memoryUsage;
	}

	public void setMemoryUsage(MemoryUsage memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public double getUsageRatio() {
		return usageRatio;
	}

	public void setUsageRatio(double usageRatio) {
		this.usageRatio = usageRatio;
	}

	public JvmMemoryUsage(MemoryUsage memoryUsage,double usageRatio) {
		this.memoryUsage = memoryUsage;
		this.usageRatio = usageRatio;
	}
}
