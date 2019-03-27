package com.gb.apm.kafka.stream.apm.common.rest;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import com.gb.apm.kafka.stream.apm.common.rest.RestExceptionMapper.CustomRestAPIException;


@Provider
public class RestMessageBodyWriterInterceptor implements WriterInterceptor {

	@Override
	public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
		context.setGenericType(ResponseBody.class);
		if(context.getEntity() instanceof CustomRestAPIException) {
			context.proceed();
		}
		context.setEntity(ResponseBody.build().setCode(0).setResponse(context.getEntity()));
		context.proceed();
	}
	
	
	static class ResponseBody {
		private int code;
		private Object response;
		
		public static ResponseBody build() {
			return new ResponseBody();
		}
		
		public int getCode() {
			return code;
		}
		public ResponseBody setCode(int code) {
			this.code = code;
			return this;
		}
		public Object getResponse() {
			return response;
		}
		public ResponseBody setResponse(Object response) {
			this.response = response;
			return this;
		}
	}

}
