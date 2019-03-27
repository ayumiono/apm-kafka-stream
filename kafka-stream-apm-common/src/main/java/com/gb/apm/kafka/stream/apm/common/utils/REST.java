package com.gb.apm.kafka.stream.apm.common.utils;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJacksonProvider;

import com.gb.apm.kafka.stream.apm.common.model.CodeMetrics;

public class REST {
	public static final Client client = ResteasyClientBuilder.newBuilder().register(ResteasyJacksonProvider.class).build();
	
	public static <T> T get(String host, int port, String path, Class<T> clazz) {
		return client.target(String.format("http://%s:%d/%s", host, port, path))
		        .request(MediaType.APPLICATION_JSON_TYPE)
		        .get(clazz);
	}
	
	public static <T> T delete(String host, int port, String path, Class<T> clazz) {
		return client.target(String.format("http://%s:%d/%s", host, port, path))
				.request(MediaType.APPLICATION_JSON_TYPE)
				.delete(clazz);
	}
	
	public static void main(String[] args) {
		@SuppressWarnings("unchecked")
		List<CodeMetrics> response = get("localhost", 8080, "code/metrics/1",List.class);
		System.out.println(response);
	}
}
