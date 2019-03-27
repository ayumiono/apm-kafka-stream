package com.gb.apm.kafka.stream.apm.common.stream;

import java.util.Map;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windows;
import org.apache.kafka.streams.kstream.internals.KStreamWindowAggregate;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;

/**
 * 
 * 需求：key重新映射
 * 
 * 原生的kStream.groupBy(KeyValueMapper<k,v,kr> selector,keyserde,valueserde)虽然支持key重新映射，但会生成sink节点，影响性能
 * 
 * 重用{@link KStreamWindowAggregate}中的大部分代码
 * 移除TupleForwarder功能，没有下游节点，所以不需要context.forward 修改process逻辑
 * 增加一个custominitaillizer.apply(startMs,endMs)来替代老的initailizer.apply()
 * 
 * 注意：使用这个Processor后续不能再有子Processor
 * 
 * @author xuelong.chen
 * @param <W>
 */
public class CustomWindowAggregator<K, V, VR, T, W extends Window> implements ProcessorSupplier<K, V> {

	private String storeName;
	private Windows<W> windows;
	private Aggregator<VR, V, T> aggregator;
	private KeyValueMapper<K, V, VR> kvMapper;
	private CustomInitiallizer<V, T> initiallizer;
	private CustomStateStoreEventListener<VR, T, W> listener;

	public CustomWindowAggregator(String storeName, Windows<W> windows, Aggregator<VR, V, T> aggregator,
			KeyValueMapper<K, V, VR> kvMapper, CustomInitiallizer<V, T> initiallizer,
			CustomStateStoreEventListener<VR, T, W> listener) {
		this.storeName = storeName;
		this.windows = windows;
		this.aggregator = aggregator;
		this.kvMapper = kvMapper;
		this.initiallizer = initiallizer;
		this.listener = listener == null ? new NothingListener() : listener;
	}

	@Override
	public Processor<K, V> get() {
		return new MethodMetricsProcessorMethodLevel();
	}

	class MethodMetricsProcessorMethodLevel extends AbstractProcessor<K, V> {
		private WindowStore<VR, T> windowStore;

		@SuppressWarnings("unchecked")
		@Override
		public void init(ProcessorContext context) {
			super.init(context);
			windowStore = (WindowStore<VR, T>) context.getStateStore(storeName);
		}

		@Override
		public void process(K key, V value) {
			if (key == null || value == null)
				return;
			VR newKey = kvMapper.apply(key, value);
			if(newKey == null) return;

			long timestamp = context().timestamp();
			Map<Long, W> matchedWindows = windows.windowsFor(timestamp);

			long timeFrom = Long.MAX_VALUE;
			long timeTo = Long.MIN_VALUE;

			for (long windowStartMs : matchedWindows.keySet()) {
				timeFrom = windowStartMs < timeFrom ? windowStartMs : timeFrom;
				timeTo = windowStartMs > timeTo ? windowStartMs : timeTo;
			}

			try (WindowStoreIterator<T> iter = windowStore.fetch(newKey, timeFrom, timeTo)) {
				while (iter.hasNext()) {
					KeyValue<Long, T> entry = iter.next();
					W window = matchedWindows.get(entry.key);
					if (window != null) {
						T oldAgg = entry.value;
						if (oldAgg == null) {
							oldAgg = initiallizer.apply(value, window.start(), window.end());
							if(oldAgg == null) return;
						}
						T newAgg = aggregator.apply(newKey, value, oldAgg);
						windowStore.put(newKey, newAgg, window.start());
						listener.whenUpdate(newKey, newAgg, window);
						matchedWindows.remove(entry.key);
					}
				}
			}

			for (long windowStartMs : matchedWindows.keySet()) {
				W window = matchedWindows.get(windowStartMs);
				T oldAgg = initiallizer.apply(value, window.start(),window.end());
				if(oldAgg == null) return;
				T newAgg = aggregator.apply(newKey, value, oldAgg);
				windowStore.put(newKey, newAgg, windowStartMs);
				listener.whenInsert(newKey, newAgg, window);
			}
		}
	}

	class NothingListener implements CustomStateStoreEventListener<VR, T, W> {

		@Override
		public void whenUpdate(VR key, T value, W window) {

		}

		@Override
		public void whenInsert(VR key, T value, W window) {

		}

	}
}
