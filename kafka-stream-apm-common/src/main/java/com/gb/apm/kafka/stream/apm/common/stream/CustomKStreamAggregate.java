package com.gb.apm.kafka.stream.apm.common.stream;

import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.internals.KStreamAggProcessorSupplier;
import org.apache.kafka.streams.kstream.internals.KTableValueGetter;
import org.apache.kafka.streams.kstream.internals.KTableValueGetterSupplier;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

public class CustomKStreamAggregate<K, V, VR, T> implements KStreamAggProcessorSupplier<K, K, V, T> {

    private final String storeName;
    private final Initializer<T> initializer;
    private final Aggregator<? super K, ? super V, T> aggregator;
    private final KeyValueMapper<K, V, VR> kvMapper;


    private boolean sendOldValues = false;

    public CustomKStreamAggregate(String storeName, Initializer<T> initializer, Aggregator<? super K, ? super V, T> aggregator,KeyValueMapper<K, V, VR> kvMapper) {
        this.storeName = storeName;
        this.initializer = initializer;
        this.aggregator = aggregator;
        this.kvMapper = kvMapper;
    }

    @Override
    public Processor<K, V> get() {
        return new KStreamAggregateProcessor();
    }

    @Override
    public void enableSendingOldValues() {
        sendOldValues = true;
    }

    private class KStreamAggregateProcessor extends AbstractProcessor<K, V> {

        private KeyValueStore<VR, T> store;
        private TupleForwarder<VR, T> tupleForwarder;

        @SuppressWarnings("unchecked")
        @Override
        public void init(ProcessorContext context) {
            super.init(context);
            store = (KeyValueStore<VR, T>) context.getStateStore(storeName);
            tupleForwarder = new TupleForwarder<>(store, context, new ForwardingCacheFlushListener<K, V>(context, sendOldValues), sendOldValues);
        }


        @Override
        public void process(K key, V value) {
        	if (key == null || value == null)
				return;
        	
        	VR newKey = kvMapper.apply(key, value);
        	
            T oldAgg = store.get(newKey);

            if (oldAgg == null)
                oldAgg = initializer.apply();

            T newAgg = oldAgg;

            // try to add the new new value
            if (value != null) {
                newAgg = aggregator.apply(key, value, newAgg);
            }

            // update the store with the new value
            store.put(newKey, newAgg);
            tupleForwarder.maybeForward(newKey, newAgg, oldAgg);
        }
    }

    @Override
    public KTableValueGetterSupplier<K, T> view() {

        return new KTableValueGetterSupplier<K, T>() {

            public KTableValueGetter<K, T> get() {
                return new KStreamAggregateValueGetter();
            }

            @Override
            public String[] storeNames() {
                return new String[]{storeName};
            }
        };
    }

    private class KStreamAggregateValueGetter implements KTableValueGetter<K, T> {

        private KeyValueStore<K, T> store;

        @SuppressWarnings("unchecked")
        @Override
        public void init(ProcessorContext context) {
            store = (KeyValueStore<K, T>) context.getStateStore(storeName);
        }

        @Override
        public T get(K key) {
            return store.get(key);
        }
    }
}
