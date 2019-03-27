package com.gb.apm.kafka.stream.apm.common.dao;

import java.util.List;

import com.gb.apm.kafka.stream.apm.common.model.v3.TraceSnapshot;
import com.gb.apm.model.TSpan;

public interface SpanDao {
	/**持久化span
	 * @param span
	 * @throws GBMongodbException
	 */
	public void storageSpan(TSpan span) throws DaoException;
	/**
	 * trace scope span
	 * @param transactionId
	 * @return
	 * @throws GBMongodbException
	 */
	public List<TSpan> traceScopeSpan(String transactionId) throws DaoException;
	/**
	 * trace scope error span
	 * @param transactionId
	 * @return
	 * @throws GBMongodbException
	 */
	public List<TSpan> traceScopeErrorSpan(String transactionId) throws DaoException;
	/**
	 * agent scope span
	 * @param agentId
	 * @return
	 * @throws GBMongodbException
	 */
	public List<TSpan> agentScopeSpan(String agentId) throws DaoException;
	/**
	 * application scope span
	 * @param applicationName
	 * @return
	 * @throws GBMongodbException
	 */
	public List<TSpan> applicationScopeSpan(String applicationName) throws DaoException;
	
	public TSpan snapshot(String transactionId);
	
	
	public List<TraceSnapshot> traceSnapshot(List<String> transactionIds);
}
