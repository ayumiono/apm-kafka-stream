package com.gb.apm.kafka.stream.apm.metrics;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

import com.gb.apm.model.TSpanEvent;

/**
 * 以stackFrame.pushTimestamp作为消息时间
 * @author xuelong.chen
 *
 */
public class MyTimestampExtractor implements TimestampExtractor {

	@Override
	public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
		if(record.value() == null) return -1L;
		TSpanEvent stackFrame = (TSpanEvent)record.value();
		return stackFrame.getStartTime();
	}
}
