package com.gb.apm.kafka.stream.apm.common.model;

import java.util.ArrayList;
import java.util.List;

import com.gb.apm.kafka.stream.apm.common.model.TreeStackFrame;
import com.gb.apm.kafka.stream.apm.common.model.utils.SHAHashUtils;
import com.google.common.base.Charsets;
import com.google.common.hash.Hasher;

/**
 * 线程栈
 * 
 * 各模型间的ID关系
 * ChainId:
 * fastHash(InvokeStack.systemId,InvokeStack.entranceMethodName)在ChainApp的"链路分析处理拓扑"中生成,最终存在chain store中
 * StackFrame:
 * fastHash(chainId,methodName) 在chainApp的"打散StackFrame处理拓扑"中生成，最终传递到apm topic中，供MethodMetrics App和持久化
 * TreeStackFrame:
 * fastHash(chainId,methodName) 在ChainApp的"链路分析处理拓扑"中生成,最终存在chain store中
 * 
 * 字段解释：
 * threadId:线程名，
 * systemName:系统名，
 * systemId:系统ID，
 * dataSign：正式数据OR测试数据
 * traceId:追踪ID，
 * _stack:方法栈帧{
 * 	className:类名，
 *  methodName:方法名，
 *  methodDesc:方法描述
 *  caller:父节点
 *  spanId:节点ID
 *  type:本地调用OR RPC设用
 * }
 * 根据stack.caller,stack.span来构造树型结构
 * {
 * 	threadId:"xxxx",
 * 	systemName:"xxxx",
 * 	systemId:xxx,
 *  dataSign:0,
 *  serverIp:xx.xx.xx.xx
 *  traceId:"xxxxxxxxxxxxxxxxxxxxxxxxxx",
 *  _stack:[
 *  	{
 *  		className:xx.xx.xx.xxxx,
 *  		methodName:xxxxx,
 *  		methodDesc:xx.xx.xx.xxxx.xxxxx(xxxxxxxx),
 *  		type:0,
 *  		order:1,
 *  		caller:0
 *  	},
 *  	{
 *  		className:xx.xx.xx.xxxx,
 *  		methodName:xxxxx,
 *  		methodDesc:xx.xx.xx.xxxx.xxxxx(xxxxxxxx),
 *  		type:0,
 *  		order:2,
 *  		caller:1
 *  	},
 *  	{
 *  		className:xx.xx.xx.xxxx,
 *  		methodName:xxxxx,
 *  		methodDesc:xx.xx.xx.xxxx.xxxxx(xxxxxxxx),
 *  		type:0,
 *  		order:3,
 *  		caller:1
 *  	},
 *  	{
 *  		className:xx.xx.xx.xxxx,
 *  		methodName:xxxxx,
 *  		methodDesc:xx.xx.xx.xxxx.xxxxx(xxxxxxxx),
 *  		type:0,
 *  		order:4,
 *  		caller:2
 *  	},
 *  ]
 * }
 *	@author xuelong.chen
 */
public class InvokeStack {
	
	private long finishTime;
	public long getFinishTime() {
		return finishTime;
	}
	public void setFinishTime(long finishTime) {
		this.finishTime = finishTime;
	}
	
	private long startTime;
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	/**
	 * 总耗时（入口方法的popTimeStamp-pushTimeStamp）
	 */
	private long spent;
	public long getSpent() {
		return spent;
	}
	public void setSpent(long spent) {
		this.spent = spent;
	}

	/**
	 * 链路正常还是异常(入口方法的exception或error不为空则为异常，否则为正常)
	 */
	private boolean errorFlag;
	public boolean getErrorFlag() {
		return errorFlag;
	}
	public void setErrorFlag(boolean errorFlag) {
		this.errorFlag = errorFlag;
	}
	
	/**
	 * 入口方法描述,用于构造链路标识(systemId+entranceMethodDesc)
	 */
	private String entranceMethodDesc;
	public String getEntranceMethodDesc() {
		return entranceMethodDesc;
	}
	public void setEntranceMethodDesc(String entranceMethodDesc) {
		this.entranceMethodDesc = entranceMethodDesc;
	}
	
	private String entranceMethod;
	public String getEntranceMethod() {
		return entranceMethod;
	}
	public void setEntranceMethod(String entranceMethod) {
		this.entranceMethod = entranceMethod;
	}
	
	private int type;
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	
	private String rpcStubMethodDesc;
	public String getRpcStubMethodDesc() {
		return rpcStubMethodDesc;
	}
	public void setRpcStubMethodDesc(String rpcStubMethodDesc) {
		this.rpcStubMethodDesc = rpcStubMethodDesc;
	}

	private String threadId;
	
	private Integer systemId;
	
	private String systemName;
	
	private Integer dataSign;
	
	private String serverIp;
	
	/**
	 * 事务号,同一个事务下的所有span共享同一个traceId
	 * 必须做到跨整个服务器集群全局唯一
	 * 一种做法是applicationId+jvm启动时间戳+序列号
	 */
	private String traceId;
	
	private List<StackFrame> stacks;

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
	public String getTraceId() {
		return traceId;
	}
	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}
	public List<StackFrame> getStacks() {
		return stacks;
	}
	public void setStacks(List<StackFrame> stacks) {
		this.stacks = stacks;
	}
	/* (non-Javadoc)
	 * 
	 * 返回形如framemethod1_framemethod2_framemethod3的字符串
	 * @see java.lang.Object#toString()
	 */
	public long treeHash() {
		Hasher her = SHAHashUtils.createHasher();
		for(StackFrame frame : stacks) {
			her.putString(frame.getMethodName(), Charsets.UTF_8);
		}
		return her.hash().asLong();
	}
	
	/**
	 * 将systemId entranceMethodDesc type
	 * sha hash生成long id
	 * @return
	 */
	public long chainIdHash() {
		return SHAHashUtils.unsignedLongHash(systemId,entranceMethodDesc,type,rpcStubMethodDesc);
	}
	
	public static List<TreeStackFrame> generateTree(InvokeStack source,long chainId){
		List<TreeStackFrame> result = new ArrayList<>(source.getStacks().size());
		for(StackFrame s : source.getStacks()) {
			TreeStackFrame treeNode = new TreeStackFrame();
			treeNode.setStackId(StackFrame.generateId(chainId, s.getMethodName(), s.getOrder(), s.getCaller(), s.getSpanId()));
			treeNode.setCaller(s.getCaller());
			treeNode.setClassName(s.getClassName());
			treeNode.setMethodDesc(s.getMethodDesc());
			treeNode.setMethodName(s.getMethodName());
			treeNode.setOrder(s.getOrder());
			treeNode.setSpanId(s.getSpanId());
			treeNode.setType(s.getType());
			result.add(treeNode);
		}
		return result;
	}
}
