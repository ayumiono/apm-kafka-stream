package com.gb.apm.kafka.stream.apm.common.dao;

public class GBPersistenceException extends Exception {
	private static final long serialVersionUID = 9058987505334242128L;
	
	public GBPersistenceException(String msg) {
		super(msg);
	}
	
	public GBPersistenceException(String msg,Throwable cause) {
		super(msg, cause);
	}
}
