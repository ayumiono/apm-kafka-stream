package com.gb.apm.kafka.stream.apm.chain.rest.grafana;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * grafana datasource test
 * 
 * @author xuelong.chen
 *
 */
@RestController
@CrossOrigin
public class TestRestProxy {
	
	private static final Logger logger = LoggerFactory.getLogger(TestRestProxy.class);

	@RequestMapping("/test")
	public void test() {
		logger.debug("grafana datasource test request");
	}
}
