package com.gb.apm.kafka.stream.apm.common.stream;

public interface CustomInitiallizerV2<K,V,T2> {
	T2 apply(K t,V v,long start,long end);
}
