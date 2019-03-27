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
import org.springframework.context.annotation.Bean;

import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.model.CodeMetrics;
import com.gb.apm.kafka.stream.apm.common.model.ExceptionStackTrace;
import com.gb.apm.kafka.stream.apm.common.model.MethodMetrics;
import com.gb.apm.kafka.stream.apm.common.model.StackFrame;
import com.gb.apm.kafka.stream.apm.common.serde.CommonJsonSerde;
import com.gb.apm.kafka.stream.apm.common.service.MetadataService;
import com.gb.apm.kafka.stream.apm.common.stream.CustomInitiallizer;
import com.gb.apm.kafka.stream.apm.common.stream.CustomStateStoreEventListener;
import com.gb.apm.kafka.stream.apm.common.stream.CustomWindowAggregator;
import com.gb.apm.kafka.stream.apm.metrics.MyTimestampExtractor;

/**
 * 实时统计指定方法的调用次数，平均耗时等指标: 方法指标聚合分析 系统级别code聚合分析
 */
//@Component
//@ConfigurationProperties(prefix="kafka")
public class MethodMetricsApp implements KafkaStreamApp,InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(MethodMetricsApp.class);

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
		return new MetadataService(this,new HostInfo(d[0], Integer.parseInt(d[1])));
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
		KStream<Long, StackFrame> apmStream = streamBuilder.stream(streamapp.get("stackframe.source.topic").toString(), 
				Consumed.with(new Serdes.LongSerde(),new CommonJsonSerde<>(StackFrame.class), new MyTimestampExtractor(),Topology.AutoOffsetReset.LATEST)
				);
		
		/*----------------------------------------链路级别方法统计，同一方法不同链路对应不同统计实体------------------------------------------------*/
		streamBuilder.addStateStore(Stores.windowStoreBuilder(
                Stores.persistentWindowStore(
                		METHOD_INVOKE_METRICS_STORE,
                		ONEWEEK,
                        3,
                        ONEHOUR,
                        false),
                new Serdes.LongSerde(),
                new CommonJsonSerde<>(MethodMetrics.class))
				.withCachingEnabled()
//				.withLoggingEnabled(config) FIXME
				);
		
		apmStream.process(new CustomWindowAggregator<>(
				METHOD_INVOKE_METRICS_STORE, 
				TimeWindows.of(ONEHOUR).advanceBy(ONEHOUR).until(ONEWEEK), 
				new MethodMetricsAggregator(), 
				new KeyValueMapper<Long, StackFrame, Long>() {
					@Override
					public Long apply(Long key, StackFrame value) {
						return key;
					}
				}, 
				new CustomInitiallizer<StackFrame,MethodMetrics>() {
					@Override
					public MethodMetrics apply(StackFrame value,long start, long end) {
						MethodMetrics o = new MethodMetrics();
						o.setStartMs(start);
						o.setEndMs(end);
						o.setClassName(value.getClassName());
						o.setMethodName(value.getMethodName());
						o.setMethodDesc(value.getMethodDesc());
						return o;
					}
				}, 
				new CustomStateStoreEventListener<Long, MethodMetrics, TimeWindow>() {

					@Override
					public void whenUpdate(Long key, MethodMetrics value, TimeWindow window) {
						logger.debug("ChainLevel MethodMetrics State Store Update Key:{} Value:{}", key,value);
					}
					@Override
					public void whenInsert(Long key, MethodMetrics value, TimeWindow window) {
						logger.debug("ChainLevel MethodMetrics State Store Insert Key:{} Value:{}", key,value);
					}
				}), METHOD_INVOKE_METRICS_STORE);
		
		
//		.groupByKey(new Serdes.LongSerde(), new CommonJsonSerde<>(StackFrame.class))
//		.aggregate(new MethodMetricsInitializer(), new MethodMetricsAggregator(),
//						TimeWindows.of(ONEHOUR).advanceBy(ONEHOUR).until(TOWHOUR), // 记录频率为1小时，记录周期为1小时，保留期间为2小时
//						new CommonJsonSerde<>(MethodMetrics.class), METHOD_INVOKE_METRICS_STORE)
		// .toStream((k,v)->k.key())//这一步不能少，否则直接用KTable<Windowed<String>,MethodMertics>.to的话会造成key类型不一致
		// .to(new Serdes.StringSerde(), new MethodMetricsSerde(),
		// METHOD_INVOKE_METRICS_TOPIC)
		;
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
                new CommonJsonSerde<>(CodeMetrics.class))
				.withCachingEnabled()
//				.withLoggingEnabled(config) FIXME
				);
		
		apmStream.process(new CustomWindowAggregator<>(
				CODE_METRICS_STORE, 
				TimeWindows.of(ONEHOUR).advanceBy(ONEHOUR).until(ONEWEEK), 
				new CodeMetricsAggregator(), 
				new KeyValueMapper<Long, StackFrame, Integer>() {
					@Override
					public Integer apply(Long key, StackFrame value) {
						return value.getSystemId();
					}
					
				}, 
				new CustomInitiallizer<StackFrame,CodeMetrics>() {
					@Override
					public CodeMetrics apply(StackFrame value,long start, long end) {
						CodeMetrics o = new CodeMetrics();
						o.setSystemId(value.getSystemId());
						o.setStartMs(start);
						o.setEndMs(end);
						return o;
					}
				}, 
				null), 
				CODE_METRICS_STORE);
//		.groupByKey()
//		.aggregate(new CodeMetricsInitializer(), new CodeMetricsAggregator(),
//				TimeWindows.of(ONEHOUR).advanceBy(ONEHOUR).until(TOWHOUR), // 记录频率为1小时，记录周期为1小时，保留期间为2小时
//				new CodeMetricsSerde(), CODE_METRICS_STORE)

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
                new Serdes.LongSerde(),
                new CommonJsonSerde<>(MethodMetrics.class))
				.withCachingEnabled()
//				.withLoggingEnabled(config) FIXME
				);
		
		apmStream
//		.process(new MethodMetricsProcessorMethodLevelSupplier<>(METHOD_INVOKE_METRICS_METHODLEVEL_STORE,
//				TimeWindows.of(ONEHOUR).advanceBy(ONEHOUR).until(TOWHOUR),
//				new MethodMetricsAggregator()),METHOD_INVOKE_METRICS_METHODLEVEL_STORE);
		.process(new CustomWindowAggregator<>(
				METHOD_INVOKE_METRICS_METHODLEVEL_STORE, 
				TimeWindows.of(ONEHOUR).advanceBy(ONEHOUR).until(ONEWEEK),
				new MethodMetricsAggregator(),
				new KeyValueMapper<Long, StackFrame, Long>() {
					@Override
					public Long apply(Long key, StackFrame value) {
						return value.methodLevelId();
					}
				},
				new CustomInitiallizer<StackFrame,MethodMetrics>() {
					@Override
					public MethodMetrics apply(StackFrame value,long start, long end) {
						MethodMetrics o = new MethodMetrics();
						o.setStartMs(start);
						o.setEndMs(end);
						o.setClassName(value.getClassName());
						o.setMethodName(value.getMethodName());
						o.setMethodDesc(value.getMethodDesc());
						return o;
					}
				}, new CustomStateStoreEventListener<Long, MethodMetrics, TimeWindow>() {

					@Override
					public void whenUpdate(Long key, MethodMetrics value, TimeWindow window) {
						logger.debug("MethodLevel MethodMetrics State Store Update Key:{} Value:{}", key,value);
					}
					@Override
					public void whenInsert(Long key, MethodMetrics value, TimeWindow window) {
						logger.debug("MethodLevel MethodMetrics State Store Insert Key:{} Value:{}", key,value);
					}
				}), 
				METHOD_INVOKE_METRICS_METHODLEVEL_STORE);

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

	static class CodeMetricsAggregator implements Aggregator<Integer, StackFrame, CodeMetrics> {

		@Override
		public CodeMetrics apply(Integer key, StackFrame frame, CodeMetrics aggregate) {
			String code = frame.getResponseCode();
			if (code == null) {
				aggregate.inc(null);
			} else {
				aggregate.inc(Long.parseLong(code));
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
	static class MethodMetricsAggregator implements Aggregator<Long, StackFrame, MethodMetrics> {
		private static final Logger logger = LoggerFactory.getLogger(MethodMetricsAggregator.class);

		@Override
		public MethodMetrics apply(Long aggKey, StackFrame value, MethodMetrics aggregate) {
			aggregate.setInvokeCount(aggregate.getInvokeCount() + 1);
			logger.debug("{}:invokecount-->{}", aggKey, aggregate.getInvokeCount());
			String traceId = value.getTraceId();
			Long popTime = value.getPopTimeStamp();
			Long pushTime = value.getPushTimeStamp();
			Long spent = popTime - pushTime;
			aggregate.distribute(spent);
			Long oldMaxSpent = aggregate.getMaxSpent();
			Long oldMinSpent = aggregate.getMinSpent();
			if (oldMaxSpent < spent) {
				aggregate.setMaxSpent(spent);
				aggregate.setMaxRelativeTraceId(traceId);
				logger.debug("{}:maxSpent-->{}", aggKey, aggregate.getMaxSpent());
			}
			if (oldMinSpent > spent) {
				aggregate.setMinSpent(spent);
				aggregate.setMinRelativeTraceId(traceId);
				logger.debug("{}:minSpent-->{}", aggKey, aggregate.getMinSpent());
			}
			aggregate.setAverageSpent((aggregate.getAverageSpent() * (aggregate.getInvokeCount() - 1) + spent)
					/ aggregate.getInvokeCount());
			ExceptionStackTrace<Throwable> exception = value.getException();
			ExceptionStackTrace<RuntimeException> error = value.getError();
			if (exception != null) {
				aggregate.setExceptionCount(aggregate.getExceptionCount() + 1);
				logger.debug("{}:exceptionCount-->{}", aggKey, aggregate.getExceptionCount());
			}
			if (error != null) {
				aggregate.setErrorCount(aggregate.getErrorCount() + 1);
				logger.debug("{}:errorCount-->{}", aggKey, aggregate.getErrorCount());
			}
			if (exception == null && error == null) {
				aggregate.setSuccessCount(aggregate.getSuccessCount() + 1);
				logger.debug("{}:successCount-->{}", aggKey, aggregate.getSuccessCount());
			}
			return aggregate;
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
