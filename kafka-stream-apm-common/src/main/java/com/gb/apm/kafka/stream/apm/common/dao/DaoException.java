package com.gb.apm.kafka.stream.apm.common.dao;

public class DaoException extends Exception {
	private static final long serialVersionUID = 9058987505334242128L;

	public DaoException(String msg) {
		super(msg);
	}

	public DaoException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
