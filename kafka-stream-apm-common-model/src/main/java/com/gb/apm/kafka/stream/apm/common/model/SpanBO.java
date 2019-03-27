package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;
import java.util.List;

import com.gb.apm.dapper.context.IAnnotation;
import com.gb.apm.dapper.context.Span;
import com.gb.apm.dapper.context.TraceId;
import com.gb.apm.kafka.stream.apm.common.annotation.MCollection;
import com.gb.apm.model.Annotation;

@MCollection("apm_span")
public class SpanBO implements Serializable, Span {
	
	private static final long serialVersionUID = 489900161260489660L;
	private Span innerSpan;
	private int applicationUrlKey;
	
	public SpanBO(int applicationUrlKey, Span innerSpan) {
		this.innerSpan = innerSpan;
		this.applicationUrlKey = applicationUrlKey;
	}

	public Span getInnerSpan() {
		return innerSpan;
	}

	public void setInnerSpan(Span innerSpan) {
		this.innerSpan = innerSpan;
	}

	public int getApplicationUrlKey() {
		return applicationUrlKey;
	}

	public void setApplicationUrlKey(int applicationUrlKey) {
		this.applicationUrlKey = applicationUrlKey;
	}

	@Override
	public Object attachFrameObject(Object frameObject) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public Object getFrameObject() {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public Object detachFrameObject() {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void recordTraceId(TraceId traceId) {
		throw new RuntimeException("unsupported operation!");
	}

	public void addAnnotation(Annotation annotation) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public <T> T findAnnotation(int key) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setAgentId(String agentId) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public String getTransactionId() {
		return innerSpan.getTransactionId();
	}

	@Override
	public String getAgentId() {
		return innerSpan.getAgentId();
	}

	@Override
	public void setApplicationName(String applicationName) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setAgentStartTime(long agentStartTime) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setApplicationServiceType(Short serviceType) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void markBeforeTime() {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public long getSpanId() {
		return innerSpan.getSpanId();
	}

	@Override
	public Long getParentSpanId() {
		return innerSpan.getParentSpanId();
	}

	@Override
	public void setStartTime(long startTime) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public long getStartTime() {
		return innerSpan.getStartTime();
	}

	@Override
	public void setElapsed(Integer elapsed) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public String getRpc() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRpc(String rpc) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setRemoteAddr(String remoteAddr) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setExceptionInfo(String exceptionInfo) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setExceptionInfo(int exceptionId, String exceptionInfo) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public boolean isErr() {
		return innerSpan.isErr();
	}

	@Override
	public void setErrCode(int err) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setApiId(Integer apiId) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setServiceType(Short serviceType) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setEndPoint(String endPoint) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setParentApplicationName(String parentApplicationName) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setParentApplicationType(Short parentApplicationType) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setAcceptorHost(String acceptorHost) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void setSpanEventList(List<_SpanEvent> spanEvent) {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public void markAfterTime() {
		throw new RuntimeException("unsupported operation!");
	}

	@Override
	public boolean isTimeRecording() {
		return innerSpan.isTimeRecording();
	}

	@Override
	public void addAnnotation(IAnnotation annotation) {
		throw new RuntimeException("unsupported operation!");
	}
}
