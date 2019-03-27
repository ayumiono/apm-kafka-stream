package com.gb.apm.dao.mongo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gb.apm.kafka.stream.apm.common.dao.SpanDao;
import com.gb.apm.kafka.stream.apm.common.model.v3.TraceSnapshot;
import com.gb.apm.model.TSpan;

public class SpanDaoImpl extends BaseImpl<TSpan> implements SpanDao{

	protected SpanDaoImpl() throws Exception {
		super(TSpan.class,"apm_span","spanId");
	}

	@Override
	public void storageSpan(TSpan span) throws GBMongodbException {
		use(APM_DB).insert(span);
	}

	@Override
	public List<TSpan> traceScopeSpan(String transactionId) throws GBMongodbException {
		TSpan span = new TSpan();
		span.setTransactionId(transactionId);
		return use(APM_DB).query(span);
	}

	@Override
	public List<TSpan> traceScopeErrorSpan(String transactionId) throws GBMongodbException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TSpan> agentScopeSpan(String agentId) throws GBMongodbException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TSpan> applicationScopeSpan(String applicationName) throws GBMongodbException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TSpan snapshot(String transactionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TraceSnapshot> traceSnapshot(List<String> transactionIds) {
		List<TraceSnapshot> result = new ArrayList<>();
		for(String tid : transactionIds) {
			TSpan tspan = this.snapshot(tid);
			if(tspan != null) {
				TraceSnapshot snapshot = new TraceSnapshot();
				snapshot.setTransactionId(tid);
				snapshot.setElapsed(tspan.getElapsed());
				snapshot.setError(tspan.getErr() != null);
				snapshot.setStartTime(tspan.getStartTime());
				result.add(snapshot);
			}
		}
		Collections.sort(result);
		return result;
	}

}
