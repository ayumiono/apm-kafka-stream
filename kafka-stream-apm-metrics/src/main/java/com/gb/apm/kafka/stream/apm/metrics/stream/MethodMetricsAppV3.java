package com.gb.apm.kafka.stream.apm.metrics.stream;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.gb.apm.common.trace.AnnotationKey;
import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.model.TSpanEvent;
import com.gb.apm.plugins.commbiz.CommBizConstants;
import com.gb.apm.kafka.stream.apm.common.model.v3.CodeMetricsV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.MethodMetrics;
import com.gb.apm.kafka.stream.apm.common.serde.CommonJsonSerde;
import com.gb.apm.kafka.stream.apm.common.service.MetadataService;
import com.gb.apm.kafka.stream.apm.common.stream.CustomInitiallizer;
import com.gb.apm.kafka.stream.apm.common.stream.CustomStateStoreEventListener;
import com.gb.apm.kafka.stream.apm.common.stream.CustomWindowAggregator;
import com.gb.apm.kafka.stream.apm.metrics.MyTimestampExtractor;

/**
 * 实时统计指定方法的调用次数，平均耗时等指标: 方法指标聚合分析 系统级别code聚合分析
 */
@Component
@ConfigurationProperties(prefix = "kafka")
public class MethodMetricsAppV3 implements KafkaStreamApp, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(MethodMetricsAppV3.class);

	public KafkaStreams streams;
	private StreamsConfig config;

	private Set<String> _filter = new HashSet<>();

	private Map<String, Object> streamconf;
	private Map<String, Object> streamapp;

	public Map<String, Object> getStreamconf() {
		return streamconf;
	}

	public void setStreamconf(Map<String, Object> streamconf) {
		this.streamconf = streamconf;
	}

	public Map<String, Object> getStreamapp() {
		return streamapp;
	}

	public void setStreamapp(Map<String, Object> streamapp) {
		this.streamapp = streamapp;
	}

	@Bean
	public MetadataService createMetadataService() {
		String applicationHost = streamconf.get("application.server").toString();
		String[] d = applicationHost.split(":");
		return new MetadataService(this, new HostInfo(d[0], Integer.parseInt(d[1])));
	}

	/**
	 * TODO 更高效的同步方法
	 * 
	 * @param key
	 */
	public synchronized void addFilter(String key) {
		_filter.add(key);
	}

	@Override
	public void start() {
		init();
		this.streams.start();
	}

	@Override
	public void stop() {
		this.streams.close();
	}

	@Override
	public void init() {
		this.config = new StreamsConfig(streamconf);
		StreamsBuilder streamBuilder = new StreamsBuilder();
		KStream<Integer, TSpanEvent> apmStream = streamBuilder.stream(streamapp.get("stackframe.source.topic").toString(), 
				Consumed.with(new Serdes.IntegerSerde(),new CommonJsonSerde<>(TSpanEvent.class), new MyTimestampExtractor(),Topology.AutoOffsetReset.LATEST)
				);

		/*----------------------------------------链路级别方法统计，同一方法不同链路对应不同统计实体------------------------------------------------*/
		streamBuilder.addStateStore(Stores.windowStoreBuilder(
                Stores.persistentWindowStore(
                		METHOD_INVOKE_METRICS_STORE,
                		ONEWEEK,
                        3,
                        ONEHOUR,
                        false),
                new Serdes.IntegerSerde(),
                new CommonJsonSerde<>(MethodMetrics.class))
				.withCachingEnabled()
//				.withLoggingEnabled(config) FIXME
				);
		
		apmStream.process(new CustomWindowAggregator<>(METHOD_INVOKE_METRICS_STORE,
				TimeWindows.of(ONEHOUR).advanceBy(ONEHOUR).until(ONEWEEK), new MethodMetricsAggregator(),
				new KeyValueMapper<Integer, TSpanEvent, Integer>() {
					@Override
					public Integer apply(Integer key, TSpanEvent value) {
						return key;
					}
				}, new CustomInitiallizer<TSpanEvent, MethodMetrics>() {
					@Override
					public MethodMetrics apply(TSpanEvent value, long start, long end) {
						MethodMetrics o = MethodMetrics.timebucket(Long.parseLong(streamapp.get("timebucket.window.size").toString())*60*1000);
						o.setStartMs(start);
						o.setEndMs(end);
						o.setApi(value.findAnnotation(AnnotationKey.API.getCode()));
						return o;
					}
				}, new CustomStateStoreEventListener<Integer, MethodMetrics, TimeWindow>() {

					@Override
					public void whenUpdate(Integer key, MethodMetrics value, TimeWindow window) {
						logger.debug("ChainLevel MethodMetrics State Store Update Key:{} Value:{}", key, value);
					}

					@Override
					public void whenInsert(Integer key, MethodMetrics value, TimeWindow window) {
						logger.debug("ChainLevel MethodMetrics State Store Insert Key:{} Value:{}", key, value);
					}
				}), METHOD_INVOKE_METRICS_STORE);

		/*----------------------------------------链路级别方法统计，同一方法不同链路对应不同统计实体------------------------------------------------*/

		/*----------------------------------------系统级别CODE监控------------------------------------------------------------------------------------*/
		streamBuilder.addStateStore(Stores.windowStoreBuilder(
                Stores.persistentWindowStore(
                		CODE_METRICS_STORE,
                		ONEWEEK,
                        3,
                        ONEHOUR,
                        false),
                new Serdes.IntegerSerde(),
                new CommonJsonSerde<>(CodeMetricsV2.class))
				.withCachingEnabled()
//				.withLoggingEnabled(config) FIXME
				);
		
		apmStream.process(new CustomWindowAggregator<>(CODE_METRICS_STORE,
				TimeWindows.of(ONEHOUR).advanceBy(ONEHOUR).until(ONEWEEK), new CodeMetricsAggregator(),
				new KeyValueMapper<Integer, TSpanEvent, Integer>() {
					@Override
					public Integer apply(Integer key, TSpanEvent value) {
						return Integer.parseInt(value.getAgentId());
					}
				}, new CustomInitiallizer<TSpanEvent, CodeMetricsV2>() {
					@Override
					public CodeMetricsV2 apply(TSpanEvent value, long start, long end) {
						CodeMetricsV2 o = new CodeMetricsV2();
						o.setAgentId(value.getAgentId());
						o.setStartMs(start);
						o.setEndMs(end);
						return o;
					}
				}, null), CODE_METRICS_STORE);
		;
		/*----------------------------------------系统级别CODE监控------------------------------------------------------------------------------------*/

		/*----------------------------------------方法级别方法统计，同一方法不同链路对应同一统计实体------------------------------------------------*/
		streamBuilder.addStateStore(Stores.windowStoreBuilder(
                Stores.persistentWindowStore(
                		METHOD_INVOKE_METRICS_METHODLEVEL_STORE,
                		ONEWEEK,
                        3,
                        ONEHOUR,
                        false),
                new Serdes.IntegerSerde(),
                new CommonJsonSerde<>(MethodMetrics.class))
				.withCachingEnabled()
//				.withLoggingEnabled(config) FIXME
				);
		
		apmStream.process(new CustomWindowAggregator<>(METHOD_INVOKE_METRICS_METHODLEVEL_STORE,
				TimeWindows.of(ONEHOUR).advanceBy(ONEHOUR).until(ONEWEEK), new MethodMetricsAggregator(),
				new KeyValueMapper<Integer, TSpanEvent, Integer>() {
					@Override
					public Integer apply(Integer key, TSpanEvent value) {
						return value.findAnnotation(CommBizConstants.METHOD_NODE_KEY.getCode());
					}
				}, new CustomInitiallizer<TSpanEvent, MethodMetrics>() {
					@Override
					public MethodMetrics apply(TSpanEvent value, long start, long end) {
						MethodMetrics o = MethodMetrics.timebucket(Long.parseLong(streamapp.get("timebucket.window.size").toString())*60*1000);
						o.setStartMs(start);
						o.setEndMs(end);
						o.setApi(value.findAnnotation(AnnotationKey.API.getCode()));
						return o;
					}
				}, new CustomStateStoreEventListener<Integer, MethodMetrics, TimeWindow>() {

					@Override
					public void whenUpdate(Integer key, MethodMetrics value, TimeWindow window) {
						logger.debug("MethodLevel MethodMetrics State Store Update Key:{} Value:{}", key, value);
					}

					@Override
					public void whenInsert(Integer key, MethodMetrics value, TimeWindow window) {
						logger.debug("MethodLevel MethodMetrics State Store Insert Key:{} Value:{}", key, value);
					}
				}), METHOD_INVOKE_METRICS_METHODLEVEL_STORE);

		/*----------------------------------------方法级别方法统计，同一方法不同链路对应同一统计实体------------------------------------------------*/

		this.streams = new KafkaStreams(streamBuilder.build(), config);
		this.streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.error(String.format("%s stream uncaught exception:", t.getName()), e);
			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
	}

	static class MethodMetricsInitializer implements Initializer<MethodMetrics> {
		private static final Logger logger = LoggerFactory.getLogger(MethodMetricsInitializer.class);

		@Override
		public MethodMetrics apply() {
			logger.debug("initializer aggreate bean");
			return new MethodMetrics();
		}
	}

	static class CodeMetricsAggregator implements Aggregator<Integer, TSpanEvent, CodeMetricsV2> {

		@Override
		public CodeMetricsV2 apply(Integer key, TSpanEvent frame, CodeMetricsV2 aggregate) {
			String code = frame.findAnnotation(CommBizConstants.RESPONSE_CODE.getCode());
			if (code == null) {
				aggregate.inc(null,frame.getTransactionId());
			} else {
				aggregate.inc(Long.parseLong(code),frame.getTransactionId());
			}
			return aggregate;
		}

	}

	/**
	 * 
	 * 方法聚合统计方法
	 * 
	 * @author xuelong.chen
	 *
	 */
	static class MethodMetricsAggregator implements Aggregator<Integer, TSpanEvent, MethodMetrics> {
		@Override
		public MethodMetrics apply(Integer aggKey, TSpanEvent value, MethodMetrics aggregate) {
			return aggregate.hit(value);
		}
	}

	@Override
	public KafkaStreams streams() {
		return streams;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		start();
	}
}

