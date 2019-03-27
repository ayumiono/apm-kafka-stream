package com.gb.apm.kafka.stream.apm.common.stream;

import org.apache.kafka.streams.kstream.internals.CacheFlushListener;
import org.apache.kafka.streams.kstream.internals.Change;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.internals.InternalProcessorContext;
import org.apache.kafka.streams.processor.internals.ProcessorNode;

public class ForwardingCacheFlushListener<K, V> implements CacheFlushListener<K, V> {
    private final InternalProcessorContext context;
    private final boolean sendOldValues;
    private final ProcessorNode myNode;

    ForwardingCacheFlushListener(final ProcessorContext context, final boolean sendOldValues) {
        this.context = (InternalProcessorContext) context;
        myNode = this.context.currentNode();
        this.sendOldValues = sendOldValues;
    }

    @Override
    public void apply(final K key, final V newValue, final V oldValue) {
        final ProcessorNode prev = context.currentNode();
        context.setCurrentNode(myNode);
        try {
            if (sendOldValues) {
                context.forward(key, new Change<>(newValue, oldValue));
            } else {
                context.forward(key, new Change<>(newValue, null));
            }
        } finally {
            context.setCurrentNode(prev);
        }
    }
}
