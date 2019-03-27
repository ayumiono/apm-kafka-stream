package com.gb.apm.web.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gb.apm.kafka.stream.apm.common.model.v3.Service;
import com.gb.apm.kafka.stream.apm.common.model.v3.ServiceLinkMetaData;

@RestController
@CrossOrigin
public class ServiceController {
	
	@Autowired
	TransportClient client;
	
	/**
	 * 查询服务依赖拓扑
	 * @param serviceId
	 * @return
	 */
	@RequestMapping("/service/topology/{serviceId}")
	public ServiceLinkMetaData topology(@PathVariable("serviceId") String serviceId) {
		try {
			GetResponse response = client.prepareGet("gb_apm_service_link", "service_link", serviceId).execute().get();
			response.getSourceAsString();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 查询指定agent下的所有服务
	 * @param agentId
	 * @return
	 */
	@RequestMapping("/services/in/{agentId}")
	public List<Service> service(@PathVariable("agentId") String agentId) {
		return null;
	}
}
