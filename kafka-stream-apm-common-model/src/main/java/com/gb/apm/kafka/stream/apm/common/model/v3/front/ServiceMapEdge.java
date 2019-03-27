package com.gb.apm.kafka.stream.apm.common.model.v3.front;

public class ServiceMapEdge {
	private String id;
	private int target;
	private int source;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getTarget() {
		return target;
	}
	public void setTarget(int target) {
		this.target = target;
	}
	public int getSource() {
		return source;
	}
	public void setSource(int source) {
		this.source = source;
	}
}
