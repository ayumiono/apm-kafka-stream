package com.gb.apm.kafka.stream.apm.common.stream;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorSupplier;

public class KStreamMap<K, V, K1, V1> implements ProcessorSupplier<K, V> {

    private final KeyValueMapper<? super K, ? super V, ? extends KeyValue<? extends K1, ? extends V1>> mapper;

    public KStreamMap(KeyValueMapper<? super K, ? super V, ? extends KeyValue<? extends K1, ? extends V1>> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Processor<K, V> get() {
        return new KStreamMapProcessor();
    }

    private class KStreamMapProcessor extends AbstractProcessor<K, V> {
        @Override
        public void process(K key, V value) {
            KeyValue<? extends K1, ? extends V1> newPair = mapper.apply(key, value);
            context().forward(newPair.key, newPair.value);
        }
    }
}
