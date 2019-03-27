package com.gb.apm.dao.mongo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBQuery.Query;
import org.springframework.stereotype.Repository;

import com.gb.apm.kafka.stream.apm.common.dao.GBApmDao;
import com.gb.apm.kafka.stream.apm.common.model.StackFrame;

@Repository
public class GBApmDaoImpl extends BaseImpl<StackFrame> implements GBApmDao {
	
	public GBApmDaoImpl() throws Exception {
		super(StackFrame.class);
	}

	@Override
	public List<StackFrame> querySpanByTraceId(String traceId) throws GBMongodbException {
		return use(APM_DB).query(Collections.singletonMap("traceId", traceId));
	}

	@Override
	public Map<String, List<StackFrame>> queryErrorTrace(long methodId,long start,long end) {
		Map<String, List<StackFrame>> rs = new HashMap<>();
		Query query = DBQuery.empty();
		query.is("methodId", methodId).is("isError", true).greaterThan("pushTimeStamp", start).lessThan("pushTimeStamp", end);
		DBCursor<StackFrame> cursor = use(APM_DB).getCollection().find(query);
		cursor.forEach(new Consumer<StackFrame>() {
			@Override
			public void accept(StackFrame t) {
				String traceId = t.getTraceId();
				List<StackFrame> sub = new ArrayList<>();
				rs.put(traceId, sub);
				Query query = DBQuery.empty();
				query.is("traceId", t.getTraceId());
				DBCursor<StackFrame> cursor = getCollection().find(query);
				cursor.forEach(new Consumer<StackFrame>() {
					@Override
					public void accept(StackFrame t) {
						sub.add(t);
					}
				});
			}
		});
		return rs;
	}

	@Override
	public Map<String, List<StackFrame>> queryExceptionTrace(long methodId,long start,long end) {
		Map<String, List<StackFrame>> rs = new HashMap<>();
		Query query = DBQuery.empty();
		query.is("methodId", methodId).is("isException", true).greaterThan("pushTimeStamp", start).lessThan("pushTimeStamp", end);
		DBCursor<StackFrame> cursor = use("babysitter").getCollection().find(query);
		cursor.forEach(new Consumer<StackFrame>() {
			@Override
			public void accept(StackFrame t) {
				String traceId = t.getTraceId();
				List<StackFrame> sub = new ArrayList<>();
				rs.put(traceId, sub);
				Query query = DBQuery.empty();
				query.is("traceId", t.getTraceId());
				DBCursor<StackFrame> cursor = getCollection().find(query);
				cursor.forEach(new Consumer<StackFrame>() {
					@Override
					public void accept(StackFrame t) {
						sub.add(t);
					}
				});
			}
		});
		return rs;
	}
}
