package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;

public class StackFrameView implements Serializable {

	private static final long serialVersionUID = 648550907170166459L;
	
	
	/**
	 * 开始时间点距invokestack开始执行时间点的时长百分比
	 */
	private long lPercent;
	/**
	 * 占总时长的百分比
	 */
	private long percentage;

	private boolean entrance;

	private String threadId;

	private Integer systemId;

	private String systemName;

	private Integer dataSign;

	private String serverIp;

	private String responseCode;

	private String message;

	/**
	 * 类名
	 */
	private String className;
	/**
	 * 方法名
	 */
	private String methodName;
	/**
	 * 方法描述符
	 */
	private String methodDesc;
	/**
	 * 标识本地方法还是RPC方法
	 */
	private int type;

	/**
	 * 入栈的序列号
	 */
	private int order;
	/**
	 * 方法调用参数
	 */
	private Object[] params;

	private String traceId;

	/**
	 * 如果所有调用都是基于本地方法，则spanId可以认为是和order是一样的
	 * 如果方法链中包含有RPC方法，则spanId必须要串联起RPC服务端和客户端的spanId
	 */
	private String spanId;

	/**
	 * 入栈时间戳
	 */
	long pushTimeStamp;
	/**
	 * 出栈时间戳
	 */
	long popTimeStamp;

	/**
	 * 是return不是athrow
	 */
	private int optcode;
	/**
	 * 如是optcode为athrow，记录下抛出的错误类型
	 */
	private ExceptionStackTrace<Throwable> exception;
	private boolean isException;

	private ExceptionStackTrace<RuntimeException> error;
	private boolean isError;
	/**
	 * 方法返回值
	 */
	private Object result;

	/**
	 * 用于关联调用者 取到入栈前的栈顶栈帧的spanId即为调用者
	 */
	private String caller;// parentId

	public long getlPercent() {
		return lPercent;
	}

	public void setlPercent(long lPercent) {
		this.lPercent = lPercent;
	}

	public long getPercentage() {
		return percentage;
	}

	public void setPercentage(long percentage) {
		this.percentage = percentage;
	}

	public boolean isEntrance() {
		return entrance;
	}

	public void setEntrance(boolean entrance) {
		this.entrance = entrance;
	}

	public String getThreadId() {
		return threadId;
	}

	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}

	public Integer getSystemId() {
		return systemId;
	}

	public void setSystemId(Integer systemId) {
		this.systemId = systemId;
	}

	public String getSystemName() {
		return systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	public Integer getDataSign() {
		return dataSign;
	}

	public void setDataSign(Integer dataSign) {
		this.dataSign = dataSign;
	}

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	public String getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getMethodDesc() {
		return methodDesc;
	}

	public void setMethodDesc(String methodDesc) {
		this.methodDesc = methodDesc;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public Object[] getParams() {
		return params;
	}

	public void setParams(Object[] params) {
		this.params = params;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getSpanId() {
		return spanId;
	}

	public void setSpanId(String spanId) {
		this.spanId = spanId;
	}

	public long getPushTimeStamp() {
		return pushTimeStamp;
	}

	public void setPushTimeStamp(long pushTimeStamp) {
		this.pushTimeStamp = pushTimeStamp;
	}

	public long getPopTimeStamp() {
		return popTimeStamp;
	}

	public void setPopTimeStamp(long popTimeStamp) {
		this.popTimeStamp = popTimeStamp;
	}

	public int getOptcode() {
		return optcode;
	}

	public void setOptcode(int optcode) {
		this.optcode = optcode;
	}

	public ExceptionStackTrace<Throwable> getException() {
		return exception;
	}

	public void setException(ExceptionStackTrace<Throwable> exception) {
		this.exception = exception;
	}

	public boolean isException() {
		return isException;
	}

	public void setException(boolean isException) {
		this.isException = isException;
	}

	public ExceptionStackTrace<RuntimeException> getError() {
		return error;
	}

	public void setError(ExceptionStackTrace<RuntimeException> error) {
		this.error = error;
	}

	public boolean isError() {
		return isError;
	}

	public void setError(boolean isError) {
		this.isError = isError;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public String getCaller() {
		return caller;
	}

	public void setCaller(String caller) {
		this.caller = caller;
	}

	
	public static StackFrameView view(StackFrame frame) {
		StackFrameView view = new StackFrameView();
		view.setCaller(frame.getCaller());
		view.setClassName(frame.getClassName());
		view.setDataSign(frame.getDataSign());
		view.setEntrance(frame.isEntrance());
		view.setError(frame.getError());
		view.setError(frame.getIsError());
		view.setException(frame.getIsException());
		view.setException(frame.getException());
		view.setMessage(frame.getMessage());
		view.setMethodDesc(frame.getMethodDesc());
		view.setMethodName(frame.getMethodName());
		view.setOptcode(frame.getOptcode());
		view.setOrder(frame.getOrder());
		view.setParams(frame.getParams());
		view.setPopTimeStamp(frame.getPopTimeStamp());
		view.setPushTimeStamp(frame.getPushTimeStamp());
		view.setResponseCode(frame.getResponseCode());
		view.setResult(frame.getResult());
		view.setServerIp(frame.getServerIp());
		view.setSpanId(frame.getSpanId());
		view.setSystemId(frame.getSystemId());
		view.setSystemName(frame.getSystemName());
		view.setThreadId(frame.getThreadId());
		view.setTraceId(frame.getTraceId());
		view.setType(frame.getType());
		return view;
	}
}
