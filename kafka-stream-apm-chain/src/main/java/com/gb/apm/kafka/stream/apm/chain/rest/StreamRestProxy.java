package com.gb.apm.kafka.stream.apm.chain.rest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.gb.apm.kafka.stream.apm.common.model.HostStoreInfo;
import com.gb.apm.kafka.stream.apm.common.service.MetadataService;

//@RequestMapping("/streams")
public class StreamRestProxy {
	
	private MetadataService metadataService;
	private KafkaStreams streams;
	
	public StreamRestProxy(KafkaStreams streams,MetadataService metadataService) {
		this.metadataService = metadataService;
		this.streams = streams;
	}
	
	/**
	 * Get the metadata for all of the instances of this Kafka Streams application
	 * 
	 * @return List of {@link HostStoreInfo}
	 */
	@RequestMapping(value="/instances",method=RequestMethod.GET)
	public List<HostStoreInfo> streamsMetadata() {
		return metadataService.streamsMetadata();
	}

	/**
	 * Get the metadata for all instances of this Kafka Streams application that
	 * currently has the provided store.
	 * 
	 * @param store
	 *            The store to locate
	 * @return List of {@link HostStoreInfo}
	 */
	@RequestMapping(value="/instances/{storeName}",method=RequestMethod.GET)
	public List<HostStoreInfo> streamsMetadataForStore(@PathVariable("storeName") String store) {
		return metadataService.streamsMetadataForStore(store);
	}

	/**
	 * Find the metadata for the instance of this Kafka Streams Application that has
	 * the given store and would have the given key if it exists.
	 * 
	 * @param store
	 *            Store to find
	 * @param key
	 *            The key to find
	 * @return {@link HostStoreInfo}
	 */
	@RequestMapping(value="/instances/{storeName}/{key}",method=RequestMethod.GET)
	public HostStoreInfo streamsMetadataForStoreAndKey(@PathVariable("storeName") String store,
			@PathVariable("key") String key) {
		return metadataService.streamsMetadataForStoreAndKey(store, key, new StringSerializer());
	}
	
	@RequestMapping(value="/instances/close",method=RequestMethod.DELETE)
	public Map<String, Object> closeStreams(){
		boolean r = streams.close(10000, TimeUnit.MILLISECONDS);
		if(r) return Collections.singletonMap("code", 0);
		return Collections.singletonMap("code", 9);
	}
}
