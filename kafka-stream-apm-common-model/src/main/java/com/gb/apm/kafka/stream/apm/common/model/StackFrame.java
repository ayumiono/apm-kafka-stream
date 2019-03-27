package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;

import com.gb.apm.kafka.stream.apm.common.annotation.MCollection;
import com.gb.apm.kafka.stream.apm.common.model.utils.SHAHashUtils;

@MCollection("invoke_stack_trace")
public class StackFrame implements Serializable{
	
	private static final long serialVersionUID = 1741817608604753854L;

	/**
	 * chain app 中增加了methodId
	 */
	private long methodId;
	
	private long chainId;
	
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
	
	/**
	 * 非检查性错误为APM的严重错误级别，记录下错误类型
	 * 当检查到RuntimeException时，当前线程栈中的栈帧都会调用AopInterceptor方法的error方法，记录下此RuntimeException，并出栈
	 * FIXME 这是一个不足的地方
	 */
	private ExceptionStackTrace<RuntimeException> error;
	private boolean isError;
	/**
	 * 方法返回值
	 */
	private Object result;
	
	/**
	 * 用于关联调用者
	 * 取到入栈前的栈顶栈帧的spanId即为调用者
	 */
	private String caller;//parentId

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

	public boolean getIsException() {
		return isException;
	}

	public void setIsException(boolean isException) {
		this.isException = isException;
	}

	public ExceptionStackTrace<RuntimeException> getError() {
		return error;
	}

	public void setError(ExceptionStackTrace<RuntimeException> error) {
		this.error = error;
	}

	public boolean getIsError() {
		return isError;
	}

	public void setIsError(boolean isError) {
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

	public long getMethodId() {
		return methodId;
	}

	public void setMethodId(long methodId) {
		this.methodId = methodId;
	}
	
	public long getChainId() {
		return chainId;
	}

	public void setChainId(long chainId) {
		this.chainId = chainId;
	}

	/**
	 * 生成方法级别的ID
	 * @return
	 */
	public long methodLevelId() {
		return SHAHashUtils.unsignedLongHash(this.systemId,this.methodName);
	}
	
	/**
	 * 链路级别方法id
	 * @param chainId
	 * @param methodName
	 * @param order
	 * @param caller
	 * @param spanId
	 * @return
	 */
	public static long generateId(long chainId,String methodName,int order,String caller,String spanId) {
		return SHAHashUtils.unsignedLongHash(chainId,methodName,order,spanId);
	}
	
	@Override
	public String toString() {
		return "StackFrame [methodId=" + methodId + ", threadId=" + threadId + ", systemId=" + systemId + ", systemName="
				+ systemName + ", dataSign=" + dataSign + ", serverIp=" + serverIp + ", responseCode=" + responseCode
				+ ", message=" + message + ", className=" + className + ", methodName=" + methodName + ", methodDesc="
				+ methodDesc + ", type=" + type + ", order=" + order + ", traceId=" + traceId + ", spanId=" + spanId
				+ ", pushTimeStamp=" + pushTimeStamp + ", popTimeStamp=" + popTimeStamp + ", optcode=" + optcode
				+ ", caller=" + caller + "]";
	}
}
