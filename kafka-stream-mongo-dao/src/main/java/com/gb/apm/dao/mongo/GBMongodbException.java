package com.gb.apm.dao.mongo;

import com.gb.apm.kafka.stream.apm.common.dao.DaoException;

public class GBMongodbException extends DaoException {
	
	private static final long serialVersionUID = 9058987505334242128L;
	
	public GBMongodbException(String msg) {
		super(msg);
	}
	
	public GBMongodbException(String msg,Throwable cause) {
		super(msg, cause);
	}
}
