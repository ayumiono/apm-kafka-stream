package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JVMMetricsStore implements Serializable{
	private static final long serialVersionUID = -996853174696273635L;
	
	private List<JVMMetrics> metrics = new ArrayList<>();

	public List<JVMMetrics> getMetrics() {
		return metrics;
	}

	public void setMetrics(List<JVMMetrics> metrics) {
		this.metrics = metrics;
	}
	
	public void addMetrics(JVMMetrics m) {
		metrics.add(m);
	}
	
	
}
