package com.gb.apm.kafka.stream.apm.common.stream;

public interface CustomInitiallizer<T1,T2> {
	T2 apply(T1 t,long start,long end);
}
