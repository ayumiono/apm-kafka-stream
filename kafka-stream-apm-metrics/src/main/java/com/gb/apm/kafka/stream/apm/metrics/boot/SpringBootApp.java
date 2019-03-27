package com.gb.apm.kafka.stream.apm.metrics.boot;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication(scanBasePackages= {"com.gb.soa.kafka.stream.apm","com.gb.apm.dao"})
@ImportResource("classpath:application_dubbo.xml")
public class SpringBootApp {
	public static void main(String[] args) {
		new SpringApplicationBuilder(SpringBootApp.class)
			.run(args);
	}
}
