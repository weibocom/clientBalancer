package com.weibo.api.client.balancer;

import java.util.Map;
import java.util.Set;

/**
 * 
 * Encapsulate the endpoints and counter;
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-8
 */

public interface EndpointHolder<R> {
	
	Map<String, Integer> getEndpointCounter();
	
	void addEndpoint(Endpoint<R> endpoint);
	
	void removeEndpoint(Endpoint<R> endpoint);
	
	String getHostname();
	
	int getPort();
	
	Set<Endpoint<R>> getEndpoints(String ipAddress);
}
