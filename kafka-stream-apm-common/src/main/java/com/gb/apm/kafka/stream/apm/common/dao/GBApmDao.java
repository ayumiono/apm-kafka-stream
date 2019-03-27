package com.gb.apm.kafka.stream.apm.common.dao;

import java.util.List;
import java.util.Map;

import com.gb.apm.kafka.stream.apm.common.model.StackFrame;

public interface GBApmDao {
	/**
	 * 根据traceId查询整条调用链span
	 * @param traceId
	 * @return
	 */
	List<StackFrame> querySpanByTraceId(String traceId) throws DaoException;
	
	/**
	 * 根据链路中的某一个方法节点，查询异常链路日志
	 * 逻辑：
	 * 1.根据methodId,isError,pushTimeStamp时间询到所有该方法节点中的链路traceId
	 * 2.再根据traceId反查所有所属stackFrame
	 * 3.最后拼成map返回
	 * @param methodId
	 * @return
	 */
	Map<String,List<StackFrame>> queryErrorTrace(long methodId,long start,long end) throws DaoException;
	
	/**
	 * 根据链路中的某一个方法节点，查询错误链路日志
	 * @param methodId
	 * @return
	 */
	Map<String,List<StackFrame>> queryExceptionTrace(long methodId,long start,long end) throws DaoException;
}
