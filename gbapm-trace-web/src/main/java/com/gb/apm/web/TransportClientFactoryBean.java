package com.gb.apm.web;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class TransportClientFactoryBean implements FactoryBean<TransportClient>, InitializingBean {
	
	private ClusterNodes clusterNodes = ClusterNodes.DEFAULT;
	private String clusterName = "elasticsearch";
	private Boolean clientTransportSniff = true;
	private Boolean clientIgnoreClusterName = Boolean.FALSE;
	private String clientPingTimeout = "5s";
	private String clientNodesSamplerInterval = "5s";
	private TransportClient client;
	private Map<String, String> properties;

	@Override
	public TransportClient getObject() throws Exception {
		return client;
	}

	@Override
	public Class<TransportClient> getObjectType() {
		return TransportClient.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	public void afterPropertiesSet() throws Exception {
		buildClient();
	}
	
	private void buildClient() {
		this.client = new PreBuiltTransportClient(settings());
		
		clusterNodes.getClusterNodes().forEach(new Consumer<TransportAddress>() {
			@Override
			public void accept(TransportAddress t) {
				TransportClientFactoryBean.this.client.addTransportAddress(t);
			}
		});
		client.connectedNodes();
	}
	
	private Settings settings() {
		if (properties != null) {
			return Settings.builder().putProperties(properties,Function.identity()).build();
		}
		return Settings.builder().put("cluster.name", clusterName).put("client.transport.sniff", clientTransportSniff)
				.put("client.transport.ignore_cluster_name", clientIgnoreClusterName)
				.put("client.transport.ping_timeout", clientPingTimeout)
				.put("client.transport.nodes_sampler_interval", clientNodesSamplerInterval).build();
	}

	public void setClusterNodes(String clusterNodes) {
		this.clusterNodes = ClusterNodes.of(clusterNodes);
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public void setClientTransportSniff(Boolean clientTransportSniff) {
		this.clientTransportSniff = clientTransportSniff;
	}

	public String getClientNodesSamplerInterval() {
		return clientNodesSamplerInterval;
	}

	public void setClientNodesSamplerInterval(String clientNodesSamplerInterval) {
		this.clientNodesSamplerInterval = clientNodesSamplerInterval;
	}

	public String getClientPingTimeout() {
		return clientPingTimeout;
	}

	public void setClientPingTimeout(String clientPingTimeout) {
		this.clientPingTimeout = clientPingTimeout;
	}

	public Boolean getClientIgnoreClusterName() {
		return clientIgnoreClusterName;
	}

	public void setClientIgnoreClusterName(Boolean clientIgnoreClusterName) {
		this.clientIgnoreClusterName = clientIgnoreClusterName;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

}
