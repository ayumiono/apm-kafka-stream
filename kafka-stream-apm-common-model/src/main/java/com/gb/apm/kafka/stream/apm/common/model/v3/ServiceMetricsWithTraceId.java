package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.util.HashMap;
import java.util.Map;

import com.gb.apm.model.TSpan;
import com.gb.apm.plugins.dubbo.DubboConstants;

@Deprecated
public class ServiceMetricsWithTraceId extends AbstractCommonMetrics<TSpan> {

	private static final long serialVersionUID = -1472297386446385258L;
	
	public ServiceMetricsWithTraceId() {}

	private int invokeCount;
	private int successCount;
	private int failCount;
	private long averageSpent;
	private long minSpent = Long.MAX_VALUE;
	private String minSpentTraceId;
	private long maxSpent = Long.MIN_VALUE;
	private String maxSpentTraceId;
	
	private Map<Long, Long> codeMetrics = new HashMap<>();

	@Override
	public void hit(TSpan t) {
		int spent = t.getElapsed() == null ? 0 : t.getElapsed();
		String traceId = t.getTransactionId();
		boolean isErr = (t.getErr() != null && t.getErr() == 1);
		if (this.maxSpent == 0 || this.maxSpent < spent) {
			maxSpent = spent;
			maxSpentTraceId = traceId;
		}

		if (this.minSpent > spent) {
			minSpent = spent;
			minSpentTraceId = traceId;
		}
		if (isErr) {
			failCount++;
		} else {
			successCount++;
		}
		this.averageSpent = (this.averageSpent * invokeCount + spent) / (this.invokeCount + 1);
		this.invokeCount++;
		
		if(t.getServiceType() == DubboConstants.DUBBO_PROVIDER_SERVICE_TYPE.getCode()) {
			Long code = t.findAnnotation(DubboConstants.GB_MESSAGE_PACK_CODE.getCode());
			if(code != null) {
				Long counter = codeMetrics.get(code);
				if(counter == null) {
					codeMetrics.put(code, 0L);
				}
				codeMetrics.put(code, codeMetrics.get(code)+1);
			}
		}
	}

	public int getInvokeCount() {
		return invokeCount;
	}

	public void setInvokeCount(int invokeCount) {
		this.invokeCount = invokeCount;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	public int getFailCount() {
		return failCount;
	}

	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}

	public long getAverageSpent() {
		return averageSpent;
	}

	public void setAverageSpent(long averageSpent) {
		this.averageSpent = averageSpent;
	}

	public long getMinSpent() {
		return minSpent;
	}

	public void setMinSpent(long minSpent) {
		this.minSpent = minSpent;
	}

	public String getMinSpentTraceId() {
		return minSpentTraceId;
	}

	public void setMinSpentTraceId(String minSpentTraceId) {
		this.minSpentTraceId = minSpentTraceId;
	}

	public long getMaxSpent() {
		return maxSpent;
	}

	public void setMaxSpent(long maxSpent) {
		this.maxSpent = maxSpent;
	}

	public String getMaxSpentTraceId() {
		return maxSpentTraceId;
	}

	public void setMaxSpentTraceId(String maxSpentTraceId) {
		this.maxSpentTraceId = maxSpentTraceId;
	}

	public Map<Long, Long> getCodeMetrics() {
		return codeMetrics;
	}

	public void setCodeMetrics(Map<Long, Long> codeMetrics) {
		this.codeMetrics = codeMetrics;
	}

	@Override
	public void aggregate(AbstractCommonMetrics<TSpan> that) {
		// TODO Auto-generated method stub
		
	}
}
