package com.gb.apm.kafka.stream.apm.common.model.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gb.apm.kafka.stream.apm.common.model.v3.Service;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceInstanceV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceMetrics;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceNodeV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.TimeBucket;
import com.gb.apm.model.Annotation;
import com.gb.apm.model.TSpan;

/**
 * 为了不破坏部分model中的final语义(包括kafka自身的TimeWindow,TimeWindows)，这里大量使用自定义的序列化和反序列化类
 * @author xuelong.chen
 */
public class JacksonUtils {

	private static ObjectMapper objectMapper;

	static {
		objectMapper = new ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		SimpleModule simpleModule = new SimpleModule();
		simpleModule.addKeyDeserializer(TimeWindow.class, new TimeWindowKeyDeserializer());
		simpleModule.addKeySerializer(TimeWindow.class, new TimeWindowSerializer());
		simpleModule.addDeserializer(ServiceNodeV2.class, new ServiceNode2Deserializer());
		simpleModule.addDeserializer(TimeBucket.class, new TimeBucketDeserializer());
		simpleModule.addDeserializer(ServiceInstanceV2.class, new ServiceInstanceV2Deserializer());
		simpleModule.addDeserializer(Service.class, new ServiceDeserializer());
		objectMapper.registerModule(simpleModule);
	}
	
	static class ServiceDeserializer extends JsonDeserializer<Service> {
		@Override
		public Service deserialize(JsonParser jp, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			JsonNode node = jp.getCodec().readTree(jp);
			String serviceURL = node.get("serviceUrl").asText();
			String serviceType = node.get("serviceType").asText();
			short serviceTypeCode = node.get("serviceTypeCode").shortValue();
			String agentId = node.get("agentId") == null ? null : node.get("agentId").asText();
			String applicationName = node.get("applicationName").asText();
			int id = node.get("id").intValue();
			Service service = new Service(serviceURL, serviceType, serviceTypeCode, agentId, applicationName, id);
			return service;
		}
	}

	static class ServiceNode2Deserializer extends JsonDeserializer<ServiceNodeV2> {

		@Override
		public ServiceNodeV2 deserialize(JsonParser jp, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			JsonNode node = jp.getCodec().readTree(jp);
			long startMs = node.get("startMs").longValue();
			long endMs = node.get("endMs").longValue();
			ObjectNode service_node = (ObjectNode) node.get("service");
			Service service = objectMapper.readValue(service_node.toString(), Service.class);
			ObjectNode metrics_node = (ObjectNode) node.get("metrics");
			ServiceMetrics metrics = objectMapper.readValue(metrics_node.toString(), ServiceMetrics.class);
			ObjectNode timewindow_node = (ObjectNode)node.get("timeBucketWindow");
			long sizeMs = timewindow_node.get("sizeMs").longValue();
			long advanceMs = timewindow_node.get("advanceMs").longValue();
			TimeWindows tws = TimeWindows.of(sizeMs).advanceBy(advanceMs);
			Map<String, ServiceInstanceV2> serverInstances = objectMapper.readValue(node.get("serverInstances").toString(), new TypeReference<Map<String, ServiceInstanceV2>>() {
			});
			Map<TimeWindow, TimeBucket<TSpan,ServiceMetrics>> timebuckets = objectMapper.readValue(node.get("timeBuckets").toString(), new TypeReference<Map<TimeWindow, TimeBucket<TSpan,ServiceMetrics>>>() {
			});
			ServiceNodeV2 rs = new ServiceNodeV2(service, startMs, endMs, tws, metrics);
			rs.setTimeBuckets(timebuckets);
			rs.setServerInstances(serverInstances);
			return rs;
		}
	}
	
	static class ServiceInstanceV2Deserializer extends JsonDeserializer<ServiceInstanceV2> {
		@Override
		public ServiceInstanceV2 deserialize(JsonParser jp, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			JsonNode node = jp.getCodec().readTree(jp);
			long startMs = node.get("startMs").longValue();
			long endMs = node.get("endMs").longValue();
			String endpoint = node.get("endpoint").asText();
			List<Annotation> annotations = objectMapper.readValue(node.get("annotations").toString(), new TypeReference<List<Annotation>>() {
			});
			ObjectNode metrics_node = (ObjectNode) node.get("metrics");
			ServiceMetrics metrics = objectMapper.readValue(metrics_node.toString(), ServiceMetrics.class);
			ObjectNode timewindow_node = (ObjectNode)node.get("timeBucketWindow");
			long sizeMs = timewindow_node.get("sizeMs").longValue();
			long advanceMs = timewindow_node.get("advanceMs").longValue();
			TimeWindows tws = TimeWindows.of(sizeMs).advanceBy(advanceMs);
			Map<TimeWindow, TimeBucket<TSpan,ServiceMetrics>> timebuckets = objectMapper.readValue(node.get("timeBuckets").toString(), new TypeReference<Map<TimeWindow, TimeBucket<TSpan,ServiceMetrics>>>() {
			});
			ServiceInstanceV2 instance = new ServiceInstanceV2(startMs, endMs, tws, metrics, endpoint, annotations);
			instance.setTimeBuckets(timebuckets);
			return instance;
		}
	}
	
	static class TimeBucketDeserializer extends JsonDeserializer<TimeBucket<TSpan, ServiceMetrics>> {
		@Override
		public TimeBucket<TSpan, ServiceMetrics> deserialize(JsonParser jp, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			JsonNode node = jp.getCodec().readTree(jp);
			long startMs = node.get("startMs").asLong();
			String metrics_json = node.get("metrics").toString();
			ServiceMetrics metrics = objectMapper.readValue(metrics_json,ServiceMetrics.class);
			TimeBucket<TSpan, ServiceMetrics> bucket = new TimeBucket<TSpan, ServiceMetrics>(metrics, startMs);
			return bucket;
		}
	}

	static class TimeWindowSerializer extends JsonSerializer<TimeWindow> {
		@Override
		public void serialize(TimeWindow timewindow, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			StringWriter writer = new StringWriter();
			objectMapper.writeValue(writer, timewindow);
			ObjectNode node = objectMapper.createObjectNode();
			node.put("startMs", timewindow.start());
			node.put("endMs", timewindow.end());
			jgen.writeFieldName(node.toString());
		}
	}

	static class TimeWindowKeyDeserializer extends KeyDeserializer {

		@Override
		public TimeWindow deserializeKey(String key, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			JsonNode node = objectMapper.readTree(key);
			long startMs = node.get("startMs").asLong();
			long endMs = node.get("endMs").asLong();
			return new TimeWindow(startMs, endMs);
		}
	}

	static class TimeWindowDeserializer extends JsonDeserializer<TimeWindow> {
		@Override
		public TimeWindow deserialize(JsonParser jp, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			JsonNode node = jp.getCodec().readTree(jp);
			long startMs = node.get("startMs").longValue();
			long endMs = node.get("endMs").longValue();
			return new TimeWindow(startMs, endMs);
		}
	}

	public static final String toJson(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static final byte[] toJsonbyte(Object object) {
		try {
			return objectMapper.writeValueAsBytes(object);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static final <T> T toObject(String data, Class<T> clzz) {
		try {
			return objectMapper.readValue(data, clzz);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static final <T> T toObject(byte[] data, Class<T> clzz) {
		try {
			return objectMapper.readValue(data, clzz);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
