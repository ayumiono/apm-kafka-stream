package com.gb.apm.kafka.stream.apm.common.model;

import java.io.Serializable;

/**
 * 相比StackFrame少了方法调用信息，只保留树结构信息
 * @author xuelong.chen
 *
 */
public class TreeStackFrame implements Serializable{
	
	private static final long serialVersionUID = -4597883165165767008L;

	/**
	 * 同StackFrame中的methodId
	 */
	private long stackId;
	
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
	 * 如果所有调用都是基于本地方法，则spanId可以认为是和order是一样的
	 * 如果方法链中包含有RPC方法，则spanId必须要串联起RPC服务端和客户端的spanId
	 */
	private String spanId;
	
	/**
	 * 用于关联调用者
	 * 取到入栈前的栈顶栈帧的spanId即为调用者
	 */
	private String caller;//parentId

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


	public String getSpanId() {
		return spanId;
	}

	public void setSpanId(String spanId) {
		this.spanId = spanId;
	}

	public String getCaller() {
		return caller;
	}

	public void setCaller(String caller) {
		this.caller = caller;
	}

	public long getStackId() {
		return stackId;
	}

	public void setStackId(long stackId) {
		this.stackId = stackId;
	}

	@Override
	public String toString() {
		return "TreeStackFrame [stackId=" + stackId + ", className=" + className + ", methodName=" + methodName
				+ ", methodDesc=" + methodDesc + ", type=" + type + ", order=" + order + ", spanId=" + spanId
				+ ", caller=" + caller + "]";
	}

}
