package com.gb.apm.kafka.stream.apm.common.serde;

import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gb.apm.kafka.stream.apm.common.model.utils.JacksonUtils;

/**
 * @author xuelong.chen
 * @param <T>
 */
public class CommonJsonSerde<T> implements Serde<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(CommonJsonSerde.class);
	
	private Class<T> clazz;
	private CommonJsonSerializer serializer;
	private CommonJsonDeserializer deserializer;

	public CommonJsonSerde(Class<T> clazz) {
		this.clazz = clazz;
		serializer = new CommonJsonSerializer();
		deserializer = new CommonJsonDeserializer();
	}

	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {}

	@Override
	public void close() {}

	@Override
	public Serializer<T> serializer() {
		return serializer;
	}

	@Override
	public Deserializer<T> deserializer() {
		return deserializer;
	}

	private class CommonJsonSerializer implements Serializer<T> {

		@Override
		public void configure(Map<String, ?> configs, boolean isKey) {}

		@Override
		public byte[] serialize(String topic, T data) {
			try {
				return JacksonUtils.toJsonbyte(data);
			} catch (Exception e) {
				logger.debug(e.getMessage(), e);
				return null;
			}
		}

		@Override
		public void close() {}

	}

	private class CommonJsonDeserializer implements Deserializer<T> {

		@Override
		public void configure(Map<String, ?> configs, boolean isKey) {}

		@Override
		public T deserialize(String topic, byte[] data) {
			try {
				return JacksonUtils.toObject(data, clazz);
			} catch (Throwable e) {
				logger.debug(e.getMessage(),e);
				return null;
			}
		}

		@Override
		public void close() {}
	}
}
