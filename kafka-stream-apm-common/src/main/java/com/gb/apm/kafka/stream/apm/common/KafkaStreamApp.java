package com.gb.apm.kafka.stream.apm.common;

import org.apache.kafka.streams.KafkaStreams;

public interface KafkaStreamApp {
	
	public static final String TOP_SPENT_METHOD_UNDER_SYSTEM = "gb_method_top_spent_under_system_store";
	public static final String TOP_SPENT_METHOD_UNDER_CHAIN = "gb_method_top_spent_under_chain_store";
	
	public static final String TOP_SPENT_CHAIN = "gb_chain_top_spent_store";
	
	public static final String CHAIN_METRICS_STORE = "gb_chain_metrics_store";
	public static final String CHAIN_TREE_STORE = "gb_chain_tree_store";
	public static final String CHAIN_TRANSFORM_STORE = "gb_chain_transform_store";
	
//	public static final String CHAIN_INVOKE_TOTAL_COUNT_STORE = "gb_chain_invoke_total_count_store";
	
	public static final String JVM_METRICS_STORE = "gb_jvm_metrics_store";
	
	public static final String METHOD_INVOKE_METRICS_STORE = "gb_method_metrics_store";
	public static final String METHOD_INVOKE_METRICS_METHODLEVEL_STORE = "gb_method_metrics_methodlevel_store";
	public static final String CODE_METRICS_STORE = "gb_code_metrics_store";
	
	
	
	public static final String SALE_REPORT_METRICS = "sale_report_metrics_store";
	public static final String SALE_REPORT_TIME_SERIES = "sale_report_time_series_store";
	
	public static final int ONEHOUR = 60 * 60 * 1000;
	public static final int TWOHOUR = 2 * 60 * 60 * 1000;
	public static final int ONEWEEK = 7 * 24 * 60 * 60 * 1000;
	public static final int ONEMONTH = 30 * 24 * 60 * 60 * 1000;
	public static final int ONEDAY = 24 * 60 * 60 * 1000;
	public static final int TENMIN = 10 * 60 * 1000;
	public static final int ONEMIN = 60 * 1000;
	public static final int FIVEMIN = 5 * 60 * 1000;
	public static final int FIVESEC = 5 * 1000;
	
	
	public static final String PRODUCTION_LOCAL_STORE_DIR = "/tmp/production/kafka_stream_store";
	public static final String TEST_LOCAL_STORE_DIR = "/tmp/test/kafka_stream_store";
	
	public void init();
	public void start();
	public void stop();
	public KafkaStreams streams();
}
