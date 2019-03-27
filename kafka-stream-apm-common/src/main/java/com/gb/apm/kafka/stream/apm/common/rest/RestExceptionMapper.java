package com.gb.apm.kafka.stream.apm.common.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.gb.apm.kafka.stream.apm.common.rest.RestExceptionMapper.CustomRestAPIException;


@Provider
public class RestExceptionMapper implements ExceptionMapper<CustomRestAPIException> {

	
	public static class CustomRestAPIException extends RuntimeException{
		
		public static final int MONGO_ERROR_CODE = 10001;
		public static final String MONGO_ERROR_MSG = "mongo error";
		
		public static final int UNKNOWN_ERROR_CODE = 90001;
		public static final String UNKNOW_ERROR_MSG = "unknown error";

		private static final long serialVersionUID = -1087495547618112818L;
		
		private int code;
		private String message;
		
		public CustomRestAPIException(int code,String message) {
			super();
			this.code = code;
			this.message = message;
		}
		
	}

	@Override
	public Response toResponse(CustomRestAPIException exception) {
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception).build();
	}
}
