package com.gb.apm.kafka.stream.apm.chain.stream;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.processor.StreamPartitioner;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.gb.apm.kafka.stream.apm.chain.ChainTimestampExtractor;
import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.dao.ChainTreeDao;
import com.gb.apm.kafka.stream.apm.common.model.Chain;
import com.gb.apm.kafka.stream.apm.common.model.ChainMetrics;
import com.gb.apm.kafka.stream.apm.common.model.InvokeStack;
import com.gb.apm.kafka.stream.apm.common.model.StackFrame;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceNode;
import com.gb.apm.kafka.stream.apm.common.serde.CommonJsonSerde;
import com.gb.apm.kafka.stream.apm.common.service.MetadataService;
import com.gb.apm.kafka.stream.apm.common.stream.CustomInitiallizer;
import com.gb.apm.kafka.stream.apm.common.stream.CustomStateStoreEventListener;
import com.gb.apm.kafka.stream.apm.common.stream.CustomWindowAggregator;

/**
 * 链路级别实时监控:
 * 绘制链路
 * 拆分链路到方法粒度，并发到{@link KafkaStreamApp.GB_APM_TOPIC}
 * 链路日志聚合分析
 * @author xuelong.chen
 *
 */

//@Component
//@ConfigurationProperties(prefix="kafka")
public class ChainApp implements KafkaStreamApp,InitializingBean {
	
	private static final Logger logger = LoggerFactory.getLogger(ChainApp.class);
	
	/**
	 * 使用foreach统计方法，绘制出调用链树 如果已经绘制，则跳过当前消息 TODO
	 * 不能解决存在条件分支的情况，systemid+entranceMethodDesc相同，但stackFrame树会有差异
	 * 解决办法是使用_stackFrame中所有的方法描述符取hash来做ID，不同的条件分支对应不同链路
	 * 
	 * 另一个需要解决的是map的大小问题
	 * 以及map持久化的问题
	 * @deprecated 使用RocksDB替代
	 */
	public static final ConcurrentHashMap<String, List<StackFrame>> chainTree = new ConcurrentHashMap<>();

	public KafkaStreams streams;
	private StreamsConfig config;
	@Autowired
	private ChainTreeDao chainTreeDao;
	
	private Map<String, Object> streamconf;
	private Map<String, Object> streamapp;
	
	@Bean
	public MetadataService createMetadataService() {
		String applicationHost = streamconf.get("application.server").toString();
		String[] d = applicationHost.split(":");
		return new MetadataService(this,new HostInfo(d[0], Integer.parseInt(d[1])));
	}
	
	/**
	 * REST接口控制链路绘制任务是否开启
	 */
	private AtomicBoolean globalChainSwitcher = new AtomicBoolean(true);
	
	private AtomicInteger chainUpdateInterval = new AtomicInteger(30*60*1000);
	
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

	public void setUpdateInterval(int interval) {
		chainUpdateInterval.set(interval);
	}
	
	public boolean switcherStatus() {
		return globalChainSwitcher.get();
	}
	
	/**
	 * 关闭开启链路绘制,建议链路信息基本稳定后关闭
	 * 开启链路绘制,链路绘制处理器无效处理较多，建议隔一段时间或明确需要时再开启
	 */
	public boolean switchStatus() {
		globalChainSwitcher.set(!globalChainSwitcher.get());
		boolean _tmp = globalChainSwitcher.get();
		if(_tmp) {
			logger.info("【ImportInfo】ChainApp Manually Activate The Chain Paint Processor");
		}else {
			logger.info("【ImportInfo】ChainApp Manually Shutdown The Chain Paint Processor");
		}
		
		return _tmp;
	}

	@Override
	public void init() {
		this.config = new StreamsConfig(streamconf);
		StreamsBuilder streamBuilder = new StreamsBuilder();
		KStream<String, InvokeStack> kstream = streamBuilder.stream(streamapp.get("chain.source.topic").toString(),
				Consumed.with(new Serdes.StringSerde(),new CommonJsonSerde<>(InvokeStack.class), new ChainTimestampExtractor(),Topology.AutoOffsetReset.LATEST)
				);
		
		/*---------------------------------从链路日志中分析链路信息 ,这个任务无效执行很多，外部增加一个控制开关，手动关闭--------------------------*/
		kstream.process(new ProcessorSupplier<String, InvokeStack>() {
			@Override
			public Processor<String, InvokeStack> get() {
				return new Processor<String, InvokeStack>() {
//					private KeyValueStore<Long, Chain> gbChainTreeStore;
					private AtomicBoolean _chainUpdataFlag = new AtomicBoolean(false);
					@Override
					public void init(ProcessorContext context) {
//						gbChainTreeStore = (KeyValueStore<Long, Chain>) context.getStateStore(CHAIN_TREE_STORE);
						context.schedule(30*60*1000);//每隔30分钟开始链路更新检测
					}

					@Override
					public void process(String key, InvokeStack value) {
						if(value == null) return;
						if(!globalChainSwitcher.get()) return;
//						if(value.getErrorFlag()) return;//只分析没有报错的链路
						try {
							long id = value.chainIdHash();
							long newHashCode = value.treeHash();
//							Chain cache = gbChainTreeStore.get(id);
							Chain cache = chainTreeDao.queryTreeById(id);
							if (cache == null) {
								Chain chain = new Chain(id,InvokeStack.generateTree(value,id),newHashCode,value.getSystemId(),
										value.getSystemName(),value.getEntranceMethod(),value.getEntranceMethodDesc(),value.getType(),value.getRpcStubMethodDesc());
								logger.info("解析到新的链路{}", chain);
//								gbChainTreeStore.put(id, chain);
								chainTreeDao.insertTree(chain);
							}else {
								if(_chainUpdataFlag.get()) {
									long oldHashCode = cache.getHashCode();
									if(oldHashCode != newHashCode) {
										logger.info("检测到链路{}发生变化，更新缓存。",cache.getId());
//										gbChainTreeStore.put(id, new Chain(id,InvokeStack.generateTree(value,id),newHashCode,value.getSystemId()));
										chainTreeDao.updateTree(new Chain(id,InvokeStack.generateTree(value,id),newHashCode,value.getSystemId(),
												value.getSystemName(),value.getEntranceMethod(),value.getEntranceMethodDesc(),value.getType(),value.getRpcStubMethodDesc()));
									}
								}
							}
						} catch (Exception e) {
							logger.error(String.format("链路解析失败:【InvokeStack】:%s", value), e);
						}
					}

					@Override
					public void punctuate(long timestamp) {
						boolean tmp = _chainUpdataFlag.get();
						_chainUpdataFlag.set(!_chainUpdataFlag.get());
						boolean _tmp = _chainUpdataFlag.get();
						logger.info("链路变化检测开关从【{}】切换为【{}】",tmp ? "开" : "关",_tmp ? "开" : "关");
					}

					@Override
					public void close() {
					}
				};
			}
		});
		/*------------------------------------从链路日志中分析链路信息--------------------------*/
		
		/*------------------------------------将chain packege打成stackframe颗粒 FIXME 中间数据要不要存储和log--------------------------*/
		kstream.transform(new TransformerSupplier<String, InvokeStack, KeyValue<Long,StackFrame>>() {
			@Override
			public Transformer<String, InvokeStack, KeyValue<Long, StackFrame>> get() {
				return new Transformer<String, InvokeStack, KeyValue<Long,StackFrame>>() {
					private ProcessorContext context;
					@Override
					public void init(ProcessorContext context) {
						this.context = context;
					}

					@Override
					public KeyValue<Long, StackFrame> transform(String key, InvokeStack value) {
						if(value == null) return null;
						List<StackFrame> stackFrames = value.getStacks();
						long chainId = value.chainIdHash();
						for(StackFrame frame : stackFrames) {
//							long atomicKey = frame.getMethodId(); 此时还没有生成methodId
							frame.setMethodId(StackFrame.generateId(chainId, frame.getMethodName(), frame.getOrder(),frame.getCaller(),frame.getSpanId()));
							logger.debug("打散stackFrame{}", frame.toString());
							context.forward(frame.getMethodId(), frame);
						}
						return null;
					}

					@Override
					public KeyValue<Long, StackFrame> punctuate(long timestamp) {
						return null;
					}

					@Override
					public void close() {
					}
				};
			}
		})
		.to(streamapp.get("stackframe.sink.topic").toString(), Produced.with(new Serdes.LongSerde(), new CommonJsonSerde<>(StackFrame.class), // 在使用前需要手需创建topic,分片数手动指定
				new StreamPartitioner<Long, StackFrame>() {
			//在使用前需要手需创建topic,分片数手动指定
			@Override
			public Integer partition(Long methodId, StackFrame stackFrame, int numPartitions) {
				try {
					int systemId = stackFrame.getSystemId();
					return systemId%numPartitions;//systemId对总分片数取余,便于system层面的监控
				} catch (Exception e) {
					e.printStackTrace();
					return 0;
				}
			}
		}));
		
		/*------------------------------------将chain packege打成stackframe颗粒--------------------------*/
		
		/*------------------------------------对chain 日志作聚合操作--------------------------*/
		/* 当前logstash output 到kafka没有设置Key值，这会导致同一个链路的日志打到随机的分片中去，
		 * 所以在这里重新指定一个Key值，repartition到kafka,然后再根据新的消息进行处理，
		 * 新的key将会保证同一个链路日志落到同一个分片，这样统计信息才有意义
		 * 
		 *  但是也会有副面效果：groupBy会增加kafka的负担和stream 任务的复杂性
		 *  可以在logstash中解析出systemId,entranceMethodName,指定key
		 *  两种方案权衡 TODO	
		 *  还是选择适当增加logstash的工作量，在logstash中给出key,格式为{systemId}
		*/
		streamBuilder.addStateStore(Stores.windowStoreBuilder(
                Stores.persistentWindowStore(
                		CHAIN_METRICS_STORE,
                		ONEWEEK,
                        3,
                        ONEHOUR,
                        false),
                new Serdes.IntegerSerde(),
                new CommonJsonSerde<>(ServiceNode.class))
				.withCachingEnabled()
//				.withLoggingEnabled(config) FIXME
				);
		
		kstream
		.process(new CustomWindowAggregator<>(
				CHAIN_METRICS_STORE,
				TimeWindows.of(ONEHOUR).advanceBy(ONEHOUR).until(ONEWEEK),
				new Aggregator<Long, InvokeStack, ChainMetrics>() {
					@Override
					public ChainMetrics apply(Long key, InvokeStack value, ChainMetrics aggregate) {
						aggregate.distribute(value.getSpent());
						if (value.getErrorFlag()) {
							aggregate.setFailCount(aggregate.getFailCount() + 1);
						} else {
							aggregate.setSuccessCount(aggregate.getSuccessCount() + 1);
						}
						if (aggregate.getMaxSpent() < value.getSpent()) {
							aggregate.setMaxSpent(value.getSpent());
							aggregate.setMaxSpentTraceId(value.getTraceId());
						}
						if (aggregate.getMinSpent() > value.getSpent()) {
							aggregate.setMinSpent(value.getSpent());
							aggregate.setMinSpentTraceId(value.getTraceId());
						}
						aggregate.setAverageSpent((aggregate.getAverageSpent() * aggregate.getInvokeCount() + value.getSpent())
								/ (aggregate.getInvokeCount() + 1));
						aggregate.setInvokeCount(aggregate.getInvokeCount() + 1);
						return aggregate;
					}
				}, 
				new KeyValueMapper<String, InvokeStack, Long>() {
					@Override
					public Long apply(String key, InvokeStack value) {
						return value.chainIdHash();
					}
					
				}, new CustomInitiallizer<InvokeStack,ChainMetrics>() {
					@Override
					public ChainMetrics apply(InvokeStack value,long start, long end) {
						ChainMetrics o = new ChainMetrics();
						o.setStartMs(start);
						o.setEndMs(end);
						return o;
					}
				},new CustomStateStoreEventListener<Long,ChainMetrics, TimeWindow>() {
					@Override
					public void whenUpdate(Long key, ChainMetrics value, TimeWindow window) {
						logger.debug("ChainMetrics State Store Update Key:{} Value:{}", key,value);
					}
					@Override
					public void whenInsert(Long key, ChainMetrics value, TimeWindow window) {
						logger.debug("ChainMetrics State Store Insert Key:{} Value:{}", key,value);
					}
				}), CHAIN_METRICS_STORE);
//		.aggregate(new Initializer<ChainMetrics>() {
//			@Override
//			public ChainMetrics apply() {
//				return new ChainMetrics();
//			}
//		}, new Aggregator<Long, InvokeStack, ChainMetrics>() {
//			@Override
//			public ChainMetrics apply(Long key, InvokeStack value, ChainMetrics aggregate) {
//				if (value.isErrorFlag()) {
//					aggregate.setFailCount(aggregate.getFailCount() + 1);
//				} else {
//					aggregate.setSuccessCount(aggregate.getSuccessCount() + 1);
//				}
//				if (aggregate.getMaxSpent() < value.getSpent()) {
//					aggregate.setMaxSpent(value.getSpent());
//					aggregate.setMaxSpentTraceId(value.getTraceId());
//				}
//				if (aggregate.getMinSpent() > value.getSpent()) {
//					aggregate.setMinSpent(value.getSpent());
//					aggregate.setMinSpentTraceId(value.getTraceId());
//				}
//				aggregate.setAverageSpent((aggregate.getAverageSpent() * aggregate.getInvokeCount() + value.getSpent())
//						/ (aggregate.getInvokeCount() + 1));
//				aggregate.setInvokeCount(aggregate.getInvokeCount() + 1);
//				return null;
//			}
//		}, TimeWindows.of(60 * 60 * 1000).advanceBy(60 * 60 * 1000).until(2 * 60 * 60 * 1000), new ChainMetricsSerde(), CHAIN_METRICS_STORE);
		/*------------------------------------对chain 日志作聚合操作--------------------------*/
		
		
		this.streams = new KafkaStreams(streamBuilder.build(), config);
		this.streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.error(String.format("%s stream uncaught exception:", t.getName()), e);
			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
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
	public KafkaStreams streams() {
		return this.streams;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		start();
	}
}
