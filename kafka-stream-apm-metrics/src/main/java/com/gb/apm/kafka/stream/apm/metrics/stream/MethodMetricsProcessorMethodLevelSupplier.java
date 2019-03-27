package com.gb.apm.kafka.stream.apm.metrics.stream;

import java.util.Map;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windows;
import org.apache.kafka.streams.kstream.internals.KStreamWindowAggregate;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;

import com.gb.apm.kafka.stream.apm.common.model.MethodMetrics;
import com.gb.apm.kafka.stream.apm.common.model.StackFrame;
import com.gb.apm.kafka.stream.apm.common.model.utils.SHAHashUtils;
import com.gb.apm.kafka.stream.apm.common.stream.CustomWindowAggregator;

/**
 * 
 * 方法级别统计处理器
 * 
 * 重用{@link KStreamWindowAggregate}中的大部分代码
 * 移除TupleForwarder功能，没有下游节点，所以不需要context.forward
 * 修改process逻辑
 * 不能再延用原fastHash(chainId,methodName)作为key
 * 改用fastHash(StackFrame.systemId,StackFrame.methodName)作为key
 * 
 * @author xuelong.chen
 * @param <W>
 * @deprecated 使用更通用的{@link CustomWindowAggregator}替代
 */
public class MethodMetricsProcessorMethodLevelSupplier<W extends Window>
		implements ProcessorSupplier<Long, StackFrame> {

	private String storeName;
	private Windows<W> windows;
	private Aggregator<Long, StackFrame, MethodMetrics> aggregator;

	public MethodMetricsProcessorMethodLevelSupplier(String storeName, Windows<W> windows,
			Aggregator<Long, StackFrame, MethodMetrics> aggregator) {
		this.storeName = storeName;
		this.windows = windows;
		this.aggregator = aggregator;
	}

	@Override
	public Processor<Long, StackFrame> get() {
		return new MethodMetricsProcessorMethodLevel();
	}

	class MethodMetricsProcessorMethodLevel extends AbstractProcessor<Long, StackFrame> {
		private WindowStore<Long, MethodMetrics> windowStore;

		@SuppressWarnings("unchecked")
		@Override
		public void init(ProcessorContext context) {
			super.init(context);
			windowStore = (WindowStore<Long, MethodMetrics>) context.getStateStore(storeName);
		}

		@Override
		public void process(Long key, StackFrame value) {
			if (key == null)
				return;

			Long _key =SHAHashUtils.unsignedLongHash(value.getSystemId(),value.getMethodName());

			long timestamp = context().timestamp();
			Map<Long, W> matchedWindows = windows.windowsFor(timestamp);

			long timeFrom = Long.MAX_VALUE;
			long timeTo = Long.MIN_VALUE;

			for (long windowStartMs : matchedWindows.keySet()) {
				timeFrom = windowStartMs < timeFrom ? windowStartMs : timeFrom;
				timeTo = windowStartMs > timeTo ? windowStartMs : timeTo;
			}

			try (WindowStoreIterator<MethodMetrics> iter = windowStore.fetch(_key, timeFrom, timeTo)) {
				while (iter.hasNext()) {
					KeyValue<Long, MethodMetrics> entry = iter.next();
					W window = matchedWindows.get(entry.key);
					if (window != null) {
						MethodMetrics oldAgg = entry.value;
						if (oldAgg == null) {
							oldAgg = new MethodMetrics();
							oldAgg.setStartMs(window.start());
							oldAgg.setEndMs(window.end());
						}
						MethodMetrics newAgg = aggregator.apply(_key, value, oldAgg);
						windowStore.put(_key, newAgg, window.start());
						matchedWindows.remove(entry.key);
					}
				}
			}

			for (long windowStartMs : matchedWindows.keySet()) {
				MethodMetrics oldAgg = new MethodMetrics();
				oldAgg.setStartMs(matchedWindows.get(windowStartMs).start());
				oldAgg.setEndMs(matchedWindows.get(windowStartMs).end());
				MethodMetrics newAgg = aggregator.apply(_key, value, oldAgg);
				windowStore.put(_key, newAgg, windowStartMs);
			}
		}
	}
}
