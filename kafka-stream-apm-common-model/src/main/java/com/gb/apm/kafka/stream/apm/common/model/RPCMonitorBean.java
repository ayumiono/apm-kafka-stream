package com.gb.apm.kafka.stream.apm.common.model;

public class RPCMonitorBean {
	/**
	 * 客户端请求时间
	 */
	private long rpcClientReqTime;
	
	/**
	 *服务端接到请求的时间 
	 */
	private long rpcServerRecTime;
	/**
	 * 服务端完成处理，响应的时间
	 */
	private long rpcServerRespTime;
	/**
	 * 客户端收到响应的时间
	 */
	private long rpcClientRecTime;
	/**
	 * 远程调用地址
	 */
	private String remoteAddress;
	public long getRpcClientReqTime() {
		return rpcClientReqTime;
	}
	public void setRpcClientReqTime(long rpcClientReqTime) {
		this.rpcClientReqTime = rpcClientReqTime;
	}
	public long getRpcServerRecTime() {
		return rpcServerRecTime;
	}
	public void setRpcServerRecTime(long rpcServerRecTime) {
		this.rpcServerRecTime = rpcServerRecTime;
	}
	public long getRpcServerRespTime() {
		return rpcServerRespTime;
	}
	public void setRpcServerRespTime(long rpcServerRespTime) {
		this.rpcServerRespTime = rpcServerRespTime;
	}
	public long getRpcClientRecTime() {
		return rpcClientRecTime;
	}
	public void setRpcClientRecTime(long rpcClientRecTime) {
		this.rpcClientRecTime = rpcClientRecTime;
	}
	public String getRemoteAddress() {
		return remoteAddress;
	}
	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}
}
