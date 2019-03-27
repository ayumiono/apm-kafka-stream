package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;

public class ExceptionStackTrace<T extends Throwable> implements Serializable{
	private static final long serialVersionUID = 6583974897381793737L;
	private StackTraceElement stackTrace;
	private String type;
	
	public StackTraceElement getStackTrace() {
		return stackTrace;
	}
	public void setStackTrace(StackTraceElement stackTrace) {
		this.stackTrace = stackTrace;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
}
