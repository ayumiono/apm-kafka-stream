package com.gb.apm.kafka.stream.apm.common.stream;

import java.util.Map;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.Windows;
import org.apache.kafka.streams.kstream.internals.KStreamAggProcessorSupplier;
import org.apache.kafka.streams.kstream.internals.KStreamWindowAggregate;
import org.apache.kafka.streams.kstream.internals.KTableValueGetter;
import org.apache.kafka.streams.kstream.internals.KTableValueGetterSupplier;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;

/**
 * 
 * 需求：key重新映射
 * 
 * 原生的kStream.groupBy(KeyValueMapper<k,v,kr> selector,keyserde,valueserde)虽然支持key重新映射，但会生成sink节点，影响性能
 * 
 * 重用{@link KStreamWindowAggregate}中的大部分代码
 * 增加一个custominitaillizer.apply(startMs,endMs)来替代老的initailizer.apply()
 * @author xuelong.chen
 * @param <W>
 */
public class CustomWindowAggregatorV2<K, V, VR, T, W extends Window> implements KStreamAggProcessorSupplier<K, Windowed<VR>, V, T> {

	private String storeName;
	private Windows<W> windows;	
	private Aggregator<VR, V, T> aggregator;
	private KeyValueMapper<K, V, VR> kvMapper;
	private CustomInitiallizerV2<VR, V, T> initiallizer;
	private CustomStateStoreEventListener<VR, T, W> listener;
	
	private boolean sendOldValues = false;

	public CustomWindowAggregatorV2(String storeName, Windows<W> windows, Aggregator<VR, V, T> aggregator,
			KeyValueMapper<K, V, VR> kvMapper, CustomInitiallizerV2<VR, V, T> initiallizer,
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
		return new CustomWindowAggregateProcessor();
	}

	class CustomWindowAggregateProcessor extends AbstractProcessor<K, V> {
		
		private TupleForwarder<Windowed<K>, T> tupleForwarder;
		
		private WindowStore<VR, T> windowStore;

		@SuppressWarnings("unchecked")
		@Override
		public void init(ProcessorContext context) {
			super.init(context);
			windowStore = (WindowStore<VR, T>) context.getStateStore(storeName);
			tupleForwarder = new TupleForwarder<>(windowStore, context, new ForwardingCacheFlushListener<Windowed<K>, V>(context, sendOldValues), sendOldValues);
		}

		@Override
		public void process(K key, V value) {
			if (key == null || value == null)
				return;
			VR newKey = kvMapper.apply(key, value);

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
							oldAgg = initiallizer.apply(newKey,value, window.start(), window.end());
						}
						T newAgg = aggregator.apply(newKey, value, oldAgg);
						windowStore.put(newKey, newAgg, window.start());
						tupleForwarder.maybeForward(new Windowed<>(key, window), newAgg, oldAgg);
						listener.whenUpdate(newKey, newAgg, window);
						matchedWindows.remove(entry.key);
					}
				}
			}

			for (long windowStartMs : matchedWindows.keySet()) {
				W window = matchedWindows.get(windowStartMs);
				T oldAgg = initiallizer.apply(newKey,value, window.start(),window.end());
				T newAgg = aggregator.apply(newKey, value, oldAgg);
				windowStore.put(newKey, newAgg, windowStartMs);
				listener.whenInsert(newKey, newAgg, window);
				tupleForwarder.maybeForward(new Windowed<>(key, matchedWindows.get(windowStartMs)), newAgg, oldAgg);
			}
		}
	}

	class NothingListener implements CustomStateStoreEventListener<VR, T, W> {

		@Override
		public void whenUpdate(VR key, T value, W window) {
			//do nothing
		}

		@Override
		public void whenInsert(VR key, T value, W window) {
			//do nothing
		}

	}

	@Override
	public KTableValueGetterSupplier<Windowed<VR>, T> view() {
		return new KTableValueGetterSupplier<Windowed<VR>, T>() {

            public KTableValueGetter<Windowed<VR>, T> get() {
                return new KStreamWindowAggregateValueGetter();
            }

            @Override
            public String[] storeNames() {
                return new String[]{storeName};
            }
        };
	}
	
	private class KStreamWindowAggregateValueGetter implements KTableValueGetter<Windowed<VR>, T> {

        private WindowStore<VR, T> windowStore;

        @SuppressWarnings("unchecked")
        @Override
        public void init(ProcessorContext context) {
            windowStore = (WindowStore<VR, T>) context.getStateStore(storeName);
        }

        @SuppressWarnings("unchecked")
        @Override
        public T get(Windowed<VR> windowedKey) {
        	VR key = windowedKey.key();
            W window = (W) windowedKey.window();

            // this iterator should contain at most one element
            try (WindowStoreIterator<T> iter = windowStore.fetch(key, window.start(), window.start())) {
                return iter.hasNext() ? iter.next().value : null;
            }
        }
    }

	@Override
	public void enableSendingOldValues() {
		sendOldValues = true;
	}
}
