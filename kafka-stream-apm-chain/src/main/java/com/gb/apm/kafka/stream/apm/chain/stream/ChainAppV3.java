package com.gb.apm.kafka.stream.apm.chain.stream;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.gb.apm.common.service.ServiceTypeRegistryService;
import com.gb.apm.common.trace.AnnotationKey;
import com.gb.apm.common.trace.ServiceType;
import com.gb.apm.kafka.stream.apm.chain.ChainTimestampExtractor;
import com.gb.apm.kafka.stream.apm.common.KafkaStreamApp;
import com.gb.apm.kafka.stream.apm.common.dao.GBPersistenceException;
import com.gb.apm.kafka.stream.apm.common.dao.ServiceDao;
import com.gb.apm.kafka.stream.apm.common.dao.ServiceLinkDao;
import com.gb.apm.kafka.stream.apm.common.model.v3.Service;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceLink;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceNodeV2;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceURL;
import com.gb.apm.kafka.stream.apm.common.serde.CommonJsonSerde;
import com.gb.apm.kafka.stream.apm.common.service.MetadataService;
import com.gb.apm.kafka.stream.apm.common.stream.CustomInitiallizer;
import com.gb.apm.kafka.stream.apm.common.stream.CustomStateStoreEventListener;
import com.gb.apm.kafka.stream.apm.common.stream.CustomWindowAggregator;
import com.gb.apm.model.TSpan;
import com.gb.apm.model.TSpanEvent;
import com.gb.apm.plugins.dubbo.DubboConstants;
import com.gb.apm.plugins.mysql.MySqlConstants;

/**
 * 链路级别实时监控: 绘制链路 拆分链路到方法粒度，并发到{@link KafkaStreamApp.GB_APM_TOPIC} 链路日志聚合分析
 * 
 * @author xuelong.chen
 *
 */

@Component
@ConfigurationProperties(prefix = "kafka")
public class ChainAppV3 implements KafkaStreamApp, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(ChainAppV3.class);

	@Autowired
	private ServiceLinkDao serviceLinkDao;
//	@Autowired
//	private ChainTreeDaoV2 chainTreeDao;
	@Autowired
	private ServiceDao serviceDao;
//	@Autowired
//	private SpanDao spanDao;

	@Autowired
	private ServiceTypeRegistryService serviceTypeRegistryService;

	public KafkaStreams streams;
	private StreamsConfig config;

	private Map<String, Object> streamconf;
	private Map<String, Object> streamapp;

	@Bean
	public MetadataService createMetadataService() {
		String applicationHost = streamconf.get("application.server").toString();
		String[] d = applicationHost.split(":");
		return new MetadataService(this, new HostInfo(d[0], Integer.parseInt(d[1])));
	}

	/**
	 * REST接口控制链路绘制任务是否开启
	 */
	private AtomicBoolean globalChainSwitcher = new AtomicBoolean(true);

	private AtomicInteger chainUpdateInterval = new AtomicInteger(30 * 60 * 1000);

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
	 * 关闭开启链路绘制,建议链路信息基本稳定后关闭 开启链路绘制,链路绘制处理器无效处理较多，建议隔一段时间或明确需要时再开启
	 */
	public boolean switchStatus() {
		globalChainSwitcher.set(!globalChainSwitcher.get());
		boolean _tmp = globalChainSwitcher.get();
		if (_tmp) {
			logger.info("【ImportInfo】ChainApp Manually Activate The Chain Paint Processor");
		} else {
			logger.info("【ImportInfo】ChainApp Manually Shutdown The Chain Paint Processor");
		}

		return _tmp;
	}

	@Override
	public void init() {
		/*topic订阅*/
		this.config = new StreamsConfig(streamconf);
		StreamsBuilder streamBuilder = new StreamsBuilder();
		KStream<String, TSpan> kstream = streamBuilder.stream(streamapp.get("chain.source.topic").toString(),
				Consumed.with(new Serdes.StringSerde(),new CommonJsonSerde<>(TSpan.class), new ChainTimestampExtractor(),Topology.AutoOffsetReset.LATEST)
				);

		/*topology发现*/
		kstream.process(new ProcessorSupplier<String, TSpan>() {
			@Override
			public Processor<String, TSpan> get() {
				return new ServiceTopologyProcessor();
			}
		});

		/*span -> spanevent 这部分功能暂时不上  */
//		kstream.transform(new TransformerSupplier<String, TSpan, KeyValue<Integer, TSpanEvent>>() {
//			@Override
//			public Transformer<String, TSpan, KeyValue<Integer, TSpanEvent>> get() {
//				return new StackFrameTransformer();
//			}
//		})
//		.to(streamapp.get("stackframe.sink.topic").toString(), Produced.with(new Serdes.IntegerSerde(), new CommonJsonSerde<>(TSpanEvent.class), // 在使用前需要手需创建topic,分片数手动指定
//				new StreamPartitioner<Integer, TSpanEvent>() {
//			@Override
//			public Integer partition(Integer chainMethodNodeKey, TSpanEvent stackFrame, int numPartitions) {
//				try {
//					String agentId = stackFrame.getAgentId();
//					return Integer.parseInt(agentId) % numPartitions;//agentId对总分片数取余,确保相同agent来源方法调用信息由相同的stream节点处理。便于system层面的监控
//				} catch (Exception e) {
//					e.printStackTrace();
//					return 0;
//				}
//			}
//		}));
		
		/*application node聚合*/
		streamBuilder.addStateStore(Stores.windowStoreBuilder(
                Stores.persistentWindowStore(
                		CHAIN_METRICS_STORE,
                		ONEWEEK,
                        3,
                        ONEHOUR,
                        false),
                new Serdes.IntegerSerde(),
                new CommonJsonSerde<>(ServiceNodeV2.class))
				.withCachingEnabled()
//				.withLoggingEnabled(config) FIXME
				);
		kstream.process(new CustomWindowAggregator<>(CHAIN_METRICS_STORE,
				TimeWindows.of(ONEHOUR).advanceBy(ONEHOUR).until(ONEWEEK),
				new ServiceNodeMetricsAggregator(), 
				new ServiceNodeKeyValueMapper(), 
				new ServiceNodeInitiallizer(), 
				new CustomStateStoreEventListener<Integer, ServiceNodeV2, TimeWindow>() {
					@Override
					public void whenUpdate(Integer key, ServiceNodeV2 value, TimeWindow window) {
						logger.debug("ServiceNode State Store Update Key:{} Value:{}", key, value);
					}

					@Override
					public void whenInsert(Integer key, ServiceNodeV2 value, TimeWindow window) {
						logger.debug("ServiceNode State Store Insert Key:{} Value:{}", key, value);
					}
				}), CHAIN_METRICS_STORE);

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
	
//	/**
//	 * 将Span中的SpanEvent打散，发送到Kafka，进行后续处理
//	 * @author xuelong.chen
//	 *
//	 */
//	class StackFrameTransformer implements Transformer<String, TSpan, KeyValue<Integer, TSpanEvent>> {
//		private ProcessorContext context;
//
//		@Override
//		public void init(ProcessorContext context) {
//			this.context = context;
//		}
//
//		@Override
//		public KeyValue<Integer, TSpanEvent> transform(String key, TSpan value) {
//			if (value == null)
//				return null;
//			try {
//				spanDao.storageSpan(value);
//			} catch (GBMongodbException e) {
//				e.printStackTrace();
//			}
//			Integer applicationUrlKey = null;
//			short serviceType = value.getServiceType();
//			ServiceType _serviceType = serviceTypeRegistryService.findServiceType(serviceType);
//			if (serviceType == DubboConstants.DUBBO_PROVIDER_SERVICE_TYPE.getCode()) {
//				String group = value.findAnnotation(DubboConstants.DUBBO_GROUP.getCode());
//				String version = value.findAnnotation(DubboConstants.DUBBO_VERSION.getCode());
//				applicationUrlKey = new ServiceURL(_serviceType, value.getRpc(),"group",group,"version",version).hashCode();
//			} else if(serviceType == ServiceType.USER.getCode()){
//				applicationUrlKey = new ServiceURL(_serviceType, value.findAnnotation(AnnotationKey.API.getCode())).hashCode();
//			} else {
//				//TODO
//				return null;
//			}
//			List<TSpanEvent> stackFrames = value.getTspanEventList();
//			for (TSpanEvent frame : stackFrames) {
//				String api = frame.findAnnotation(AnnotationKey.API.getCode());
//				int chainMethodNodeKey = applicationUrlKey * 31 + api.hashCode();
//				frame.addToAnnotations(new Annotation(CommBizConstants.CHAIN_METHOD_NODE_KEY.getCode(), chainMethodNodeKey+""));//将chainMethodNodeKey存到annotation中
//				frame.addToAnnotations(new Annotation(CommBizConstants.METHOD_NODE_KEY.getCode(),Chain.getHashCode(api, frame.getAnnotations(), serviceType)+""));
//				logger.debug("打散stackFrame{}", frame.toString());
//				context.forward(chainMethodNodeKey, frame);
//			}
//			return null;
//		}
//
//		@Override
//		public KeyValue<Integer, TSpanEvent> punctuate(long timestamp) {
//			return null;
//		}
//
//		@Override
//		public void close() {
//
//		}
//	}
	
	/**
	 * ApplicationNode 聚合
	 * @author xuelong.chen
	 *
	 */
	class ServiceNodeMetricsAggregator implements Aggregator<Integer, TSpan, ServiceNodeV2> {
		@Override
		public ServiceNodeV2 apply(Integer key, TSpan value, ServiceNodeV2 aggregate) {
			try {
				return (ServiceNodeV2) aggregate.hit(value,value.getStartTime());
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
				return aggregate;
			}
			
		}
	}
	
	/**
	 * ApplicationNode kv映射
	 * Service用{@code ServiceURL}}来唯一标识
	 * @author xuelong.chen
	 *
	 */
	class ServiceNodeKeyValueMapper implements KeyValueMapper<String, TSpan, Integer> {
		@Override
		public Integer apply(String key, TSpan value) {
			try {
				Integer applicationUrlKey = null;
				short serviceType = value.getServiceType();
				ServiceType _serviceType = serviceTypeRegistryService.findServiceType(serviceType);
				if (serviceType == DubboConstants.DUBBO_PROVIDER_SERVICE_TYPE.getCode()) {
//					String group = value.findAnnotation(DubboConstants.DUBBO_GROUP.getCode());
//					String version = value.findAnnotation(DubboConstants.DUBBO_VERSION.getCode());
					applicationUrlKey = new ServiceURL(_serviceType, value.getRpc()/*,"group",group,"version",version*/).hashCode();
				} else if(serviceType == ServiceType.USER.getCode()) {
					applicationUrlKey = new ServiceURL(_serviceType, value.findAnnotation(AnnotationKey.API.getCode())).hashCode();
				} else {
					//TODO 其他应用支持
					return null;
				}
				return applicationUrlKey;
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
				return null;
			}
			
		}
	}
	
	/**
	 * 
	 * ApplicationNode聚合统计类初始化
	 * @author xuelong.chen
	 *
	 */
	class ServiceNodeInitiallizer implements CustomInitiallizer<TSpan, ServiceNodeV2> {
		@Override
		public ServiceNodeV2 apply(TSpan value, long start, long end) {
			try {
				Service app = null;
				short serviceType = value.getServiceType();
				ServiceType _serviceType = serviceTypeRegistryService.findServiceType(serviceType);
				if (serviceType == DubboConstants.DUBBO_PROVIDER_SERVICE_TYPE.getCode()) {
//					String group = value.findAnnotation(DubboConstants.DUBBO_GROUP.getCode());
//					String version = value.findAnnotation(DubboConstants.DUBBO_VERSION.getCode());
					app = new Service(new ServiceURL(_serviceType, value.getRpc()/*,"group",group,"version",version*/),_serviceType, value.getAgentId(), value.getApplicationName());
				} else if(serviceType == ServiceType.USER.getCode()) {
					app = new Service(new ServiceURL(_serviceType, value.findAnnotation(AnnotationKey.API.getCode())), _serviceType, value.getAgentId(), value.getApplicationName());
				} else {
					//TODO 其他应用支持
					return null;
				}
				ServiceNodeV2 serviceNode = new ServiceNodeV2(app,start,end);
				return serviceNode;
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
				return null;
			}
			
		}
	}
	
	/**
	 * 
	 * 应用拓扑自动识别
	 * 
	 * @author xuelong.chen
	 *
	 */
	class ServiceTopologyProcessor implements Processor<String, TSpan> {
		private AtomicBoolean _chainUpdataFlag = new AtomicBoolean(true);

		@Override
		public void init(ProcessorContext context) {
			context.schedule(10 * 60 * 1000, PunctuationType.WALL_CLOCK_TIME, new Punctuator() {// 每隔10分钟开始链路更新检测
				@Override
				public void punctuate(long timestamp) {
					_chainUpdataFlag.set(!_chainUpdataFlag.get());
				}
			});
		}

		@Override
		public void process(String key, TSpan value) {
			
			if(!_chainUpdataFlag.get()) {
				return;
			}
			
			if (value == null)
				return;
			
			//应用拓扑自发现
			short serviceType = value.getServiceType();
			ServiceType _serviceType = serviceTypeRegistryService.findServiceType(serviceType);
			Service service = null;
			if (serviceType == DubboConstants.DUBBO_PROVIDER_SERVICE_TYPE.getCode()) {//FIXME 同一dubbo接口，不同的group,version被视为不同的服务
//				String group = value.findAnnotation(DubboConstants.DUBBO_GROUP.getCode());
//				String version = value.findAnnotation(DubboConstants.DUBBO_VERSION.getCode());
				service = new Service(new ServiceURL(_serviceType, value.getRpc()/*,"group",group,"version",version*/),_serviceType, value.getAgentId(), value.getApplicationName());
			} else if(serviceType == ServiceType.USER.getCode()){
				service = new Service(new ServiceURL(_serviceType, value.findAnnotation(AnnotationKey.API.getCode())), _serviceType, value.getAgentId(), value.getApplicationName());
			} else {
				//TODO 其他应用支持
				return;
			}
			
			try {
				serviceDao.addServcie(service);
			} catch (GBPersistenceException e1) {
				e1.printStackTrace();
			}
			
			Set<Integer> dependencies = new HashSet<>();
			List<TSpanEvent> stackFrames = value.getTspanEventList();
			stackFrames.sort(new Comparator<TSpanEvent>() {
				@Override
				public int compare(TSpanEvent o1, TSpanEvent o2) {
					return o1.getSequence()-o2.getSequence();
				}
			});
			for (TSpanEvent frame : stackFrames) {
				short spanEventServiceType = frame.getServiceType();
				ServiceType _spanEventServiceType = serviceTypeRegistryService.findServiceType(spanEventServiceType);
				if (spanEventServiceType == DubboConstants.DUBBO_CONSUMER_SERVICE_TYPE.getCode()) {//FIXME 同一dubbo接口，不同的group,version被视为不同的服务
					String rpc = frame.findAnnotation(DubboConstants.DUBBO_INTERFACE_ANNOTATION_KEY.getCode());
//					String group = value.findAnnotation(DubboConstants.DUBBO_GROUP.getCode());
//					String version = value.findAnnotation(DubboConstants.DUBBO_VERSION.getCode());
					ServiceType referenceAppServiceType = serviceTypeRegistryService.findServiceType(DubboConstants.DUBBO_PROVIDER_SERVICE_TYPE.getCode());
//					Service referenceDubbo = new Service(new ServiceURL(referenceAppServiceType, rpc/*,"group",group,"version",version*/),referenceAppServiceType, value.getAgentId(),/*wrong agentid**/ value.getApplicationName()/*wrong applicationname*/);
//					int dependencyKey = referenceDubbo.getId();
					int dependencyKey = new ServiceURL(referenceAppServiceType, rpc/*,"group",group,"version",version*/).hashCode();
					dependencies.add(dependencyKey);
					//FIXME 要不要先存referenceDubbo到数据库? 有些被依赖服务可能在依赖端信息不全，暂时不在依赖端生成被依赖服务
					
				}else if(spanEventServiceType == MySqlConstants.MYSQL_EXECUTE_QUERY.getCode()){
					Service referenceMYSQL = new Service(new ServiceURL(_spanEventServiceType, frame.getEndPoint(),"DataBase",frame.getDestinationId()),_spanEventServiceType,null,MySqlConstants.MYSQL.getName());
					try {//添加mysql应用
						serviceDao.addServcie(referenceMYSQL);
					} catch (GBPersistenceException e1) {
						e1.printStackTrace();
					}
					int dependencyKey = referenceMYSQL.getId();
					dependencies.add(dependencyKey);
				}else {
					//TODO 其他应用支持
				}
			}
			
			try {
				ServiceLink link = new ServiceLink(service, dependencies);
				link.setEntrance(value.getParentSpanId() == null);
				serviceLinkDao.addServiceLink(link);
			} catch (GBPersistenceException e) {
				e.printStackTrace();
			}
			
//			//暂时不上这块功能
//			try {
//				int applicationKey = service.getId();
//				Chain chain = Chain.generateChain(stackFrames, applicationKey, value.getApplicationName(), value.getServiceType());
//				chainTreeDao.insertTree(chain);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
		}

		@Override
		public void close() {
		}

		@Override
		public void punctuate(long timestamp) {
		}
	}
}
