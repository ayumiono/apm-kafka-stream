package com.gb.apm.kafka.stream.apm.chain.boot;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

import com.gb.apm.common.service.DefaultServiceTypeRegistryService;
import com.gb.apm.common.service.DefaultTraceMetadataLoaderService;
import com.gb.apm.common.service.ServiceTypeRegistryService;
import com.gb.apm.common.service.TraceMetadataLoaderService;
import com.gb.apm.common.utils.logger.StdoutCommonLoggerFactory;


@SpringBootApplication(scanBasePackages= {"com.gb.apm.kafka.stream.apm","com.gb.apm.dao"})
@ImportResource("classpath:application_dubbo.xml")
public class SpringBootApp {
	
	public static void main(String[] args) {
		new SpringApplicationBuilder(SpringBootApp.class)
			.run(args);
	}
	
	@Bean
	public TraceMetadataLoaderService traceMetadataLoader() {
		return new DefaultTraceMetadataLoaderService();
	}
	
	@Bean
	public ServiceTypeRegistryService serviceTypeRegistry(TraceMetadataLoaderService traceMetadataLoaderService) {
		return new DefaultServiceTypeRegistryService(traceMetadataLoaderService,StdoutCommonLoggerFactory.INSTANCE);
	}
}
