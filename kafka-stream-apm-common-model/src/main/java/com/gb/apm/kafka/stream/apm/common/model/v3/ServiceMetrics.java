package com.gb.apm.kafka.stream.apm.common.model.v3;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.gb.apm.model.TSpan;
import com.gb.apm.plugins.dubbo.DubboConstants;

public class ServiceMetrics extends AbstractCommonMetrics<TSpan>{
	
	private static final long serialVersionUID = 5256767546652947281L;
	
	private int invokeCount;
	private int successCount;
	private int failCount;
	private long averageSpent;
	private long minSpent = Long.MAX_VALUE;
	private long maxSpent = Long.MIN_VALUE;
	
	private Map<String, Long> codeMetrics = new HashMap<>();
	
	public ServiceMetrics() {}

	@Override
	public void hit(TSpan t) {
		int spent = t.getElapsed() == null ? 0 : t.getElapsed();
		boolean isErr = (t.getErr() != null && t.getErr() == 1);
		if (this.maxSpent == 0 || this.maxSpent < spent) {
			maxSpent = spent;
		}

		if (this.minSpent > spent) {
			minSpent = spent;
		}
		if (isErr) {
			failCount++;
		} else {
			successCount++;
		}
		this.averageSpent = (this.averageSpent * invokeCount + spent) / (this.invokeCount + 1);
		this.invokeCount++;
		
		if(t.getServiceType() == DubboConstants.DUBBO_PROVIDER_SERVICE_TYPE.getCode()) {
			String code = t.findAnnotation(DubboConstants.GB_MESSAGE_PACK_CODE.getCode());
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

	public long getMaxSpent() {
		return maxSpent;
	}

	public void setMaxSpent(long maxSpent) {
		this.maxSpent = maxSpent;
	}
	
	public Map<String, Long> getCodeMetrics() {
		return codeMetrics;
	}

	public void setCodeMetrics(Map<String, Long> codeMetrics) {
		this.codeMetrics = codeMetrics;
	}

	public static void aggregate(ServiceMetrics from, ServiceMetrics to) {
		to.setInvokeCount(to.getInvokeCount() + from.getInvokeCount());
		to.setFailCount(to.getFailCount() + from.getFailCount());
		to.setSuccessCount(to.getSuccessCount() + from.getSuccessCount());
		for(Entry<String, Long> code : from.codeMetrics.entrySet()) {
			String c = code.getKey();
			if(to.codeMetrics.containsKey(c)) {
				to.codeMetrics.put(c, to.codeMetrics.get(c) + code.getValue());
			}else {
				to.codeMetrics.put(c, code.getValue());
			}
		}
		if(to.getMaxSpent() < from.getMaxSpent()) {
			to.setMaxSpent(from.getMaxSpent());
		}
		if(to.getMinSpent() > from.getMinSpent()) {
			to.setMinSpent(from.getMinSpent());
		}
		to.setAverageSpent((to.getAverageSpent() * to.getInvokeCount() + from.getAverageSpent() * from.getInvokeCount())/(to.getInvokeCount() + from.getInvokeCount()));
	}

	@Override
	public String toString() {
		return "ServiceMetrics [invokeCount=" + invokeCount + ", successCount=" + successCount + ", failCount="
				+ failCount + ", averageSpent=" + averageSpent + ", minSpent=" + minSpent + ", maxSpent=" + maxSpent
				+ "]";
	}

	@Override
	public void aggregate(AbstractCommonMetrics<TSpan> that) {
		if(!(that instanceof ServiceMetrics)) {
			throw new IllegalArgumentException("类型不匹配");
		}
		ServiceMetrics _that = (ServiceMetrics) that;
		if(this.getStartMs() > that.getStartMs()) {
			this.setStartMs(that.getStartMs());
		}
		this.invokeCount += _that.invokeCount;
		this.failCount += _that.failCount;
		this.successCount +=_that.successCount;
		for(Entry<String, Long> code : _that.codeMetrics.entrySet()) {
			String c = code.getKey();
			if(this.codeMetrics.containsKey(c)) {
				this.codeMetrics.put(c, this.codeMetrics.get(c) + code.getValue());
			}else {
				this.codeMetrics.put(c, code.getValue());
			}
		}
		if(this.getMaxSpent() < _that.getMaxSpent()) {
			this.setMaxSpent(_that.getMaxSpent());
		}
		if(this.getMinSpent() > _that.getMinSpent()) {
			this.setMinSpent(_that.getMinSpent());
		}
		this.setAverageSpent((this.getAverageSpent() * this.getInvokeCount() + _that.getAverageSpent() * _that.getInvokeCount())/(this.getInvokeCount() + _that.getInvokeCount()));
	}
	
	

}
