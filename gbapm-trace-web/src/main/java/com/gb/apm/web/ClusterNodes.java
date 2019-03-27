package com.gb.apm.web;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.transport.TransportAddress;

public class ClusterNodes {
	
	public static ClusterNodes DEFAULT = ClusterNodes.of("127.0.0.1:9300");

	private static final String COLON = ":";
	private static final String COMMA = ",";
	
	private final List<TransportAddress> clusterNodes;
	
	public List<TransportAddress> getClusterNodes(){
		return this.clusterNodes;
	}
	
	private ClusterNodes(String addresses) {
		if(StringUtils.isBlank(addresses)) {
			throw new RuntimeException("address is blank!");
		}
		String addressArray[] = addresses.split(COMMA);
		this.clusterNodes = Arrays.stream(addressArray).map(node->{
			String addr[] = node.split(COLON);
			String host = addr[0];
			String port = addr[1];
			return new TransportAddress(new InetSocketAddress(toInetAddress(host), Integer.valueOf(port)));
		}).collect(Collectors.toList());;
	}
	
	public static ClusterNodes of(String addresses) {
		return new ClusterNodes(addresses);
	}
	
	private static InetAddress toInetAddress(String host) {

		try {
			return InetAddress.getByName(host);
		} catch (UnknownHostException o_O) {
			throw new IllegalArgumentException(o_O);
		}
	}
}
