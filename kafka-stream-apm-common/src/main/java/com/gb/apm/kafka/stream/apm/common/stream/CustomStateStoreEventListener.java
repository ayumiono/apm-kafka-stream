package com.gb.apm.kafka.stream.apm.common.stream;

public interface CustomStateStoreEventListener<K,V,W> {
	void whenUpdate(K key,V value,W window);
	void whenInsert(K key,V value,W window);
}
