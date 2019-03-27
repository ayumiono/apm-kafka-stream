package com.gb.apm.kafka.stream.apm.metrics.rest.grafana;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class TestRestProxy {

	@RequestMapping("/test")
	public void test() {
		System.out.println("test");
	}
}
