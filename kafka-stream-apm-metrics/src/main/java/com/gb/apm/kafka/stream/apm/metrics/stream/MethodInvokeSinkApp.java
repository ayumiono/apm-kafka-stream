package com.gb.apm.kafka.stream.apm.metrics.stream;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.ForeachAction;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.model.MethodMetrics;

/**
 * 监听MethodInvokeApp map过后的消息，将结果汇总到本地store中
 * @author xuelong.chen
 * @deprecated
 */
public class MethodInvokeSinkApp implements KafkaStreamApp {
	
	private static final Logger logger = LoggerFactory.getLogger(MethodInvokeSinkApp.class);

	private KafkaStreams streams;
	private StreamsConfig config;
	public Map<Windowed<String>, MethodMetrics> _cache = new HashMap<>();

	public MethodInvokeSinkApp(Properties properties) {
		this.config = new StreamsConfig(properties);
	}

	@Override
	public void start() {
		init();
		streams.start();
	}

	@Override
	public void stop() {
		streams.close();
	}

	@Override
	public void init() {
		KStreamBuilder builder = new KStreamBuilder();
		KStream<Windowed<String>, MethodMetrics> methodInvokeCountStream = builder.stream("");
		
		methodInvokeCountStream.foreach(new ForeachAction<Windowed<String>, MethodMetrics>() {// TODO 需要汇总所有来源分区中的数据
			@Override
			public void apply(Windowed<String> key, MethodMetrics value) {
				if(_cache.containsKey(key)) {
					MethodMetrics originalV = _cache.get(key);
					logger.info("开始合并分区处理结果{}到{}",value,originalV);
				}else {
					_cache.put(key, value);
				}
			}
		});
		
		
		streams = new KafkaStreams(builder, this.config);
		this.streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {

			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
	}
	
	public static void main(String[] args) {
		TimeWindows windows = TimeWindows.of(10*60*1000).advanceBy(10*60*1000);
		Map<Long, TimeWindow> _windows = windows.windowsFor(System.currentTimeMillis());
		for(Long start : _windows.keySet()) {
			System.out.println(new Date(1511741303412L));
		}
	}

	@Override
	public KafkaStreams streams() {
		return streams;
	}
}
