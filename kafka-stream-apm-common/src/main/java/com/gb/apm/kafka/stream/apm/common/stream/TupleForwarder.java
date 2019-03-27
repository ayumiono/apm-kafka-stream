package com.gb.apm.kafka.stream.apm.common.stream;

import org.apache.kafka.streams.kstream.internals.Change;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.state.internals.CachedStateStore;

public class TupleForwarder<K, V> {
    private final boolean cached;
    private final ProcessorContext context;
    private final boolean sendOldValues;

    @SuppressWarnings("unchecked")
    TupleForwarder(final StateStore store,
                   final ProcessorContext context,
                   final ForwardingCacheFlushListener flushListener,
                   final boolean sendOldValues) {
        this.cached = store instanceof CachedStateStore;
        this.context = context;
        this.sendOldValues = sendOldValues;
        if (this.cached) {
            ((CachedStateStore) store).setFlushListener(flushListener,sendOldValues);
        }
    }

    public void maybeForward(final K key,
                             final V newValue,
                             final V oldValue) {
        if (!cached) {
            if (sendOldValues) {
                context.forward(key, new Change<>(newValue, oldValue));
            } else {
                context.forward(key, new Change<>(newValue, null));
            }
        }
    }
}
