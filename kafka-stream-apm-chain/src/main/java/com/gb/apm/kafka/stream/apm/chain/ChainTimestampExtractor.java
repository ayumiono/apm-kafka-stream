package com.gb.apm.kafka.stream.apm.chain;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

import com.gb.apm.model.TSpan;

public class ChainTimestampExtractor implements TimestampExtractor {

	@Override
	public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
		if(record.value() == null) return -1L;
		return ((TSpan)record.value()).getStartTime();
	}

}
