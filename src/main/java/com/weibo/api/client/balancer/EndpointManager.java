package com.weibo.api.client.balancer;

import com.weibo.api.client.balancer.impl.EndpointBalancerConfig;

/**
 * 
 * Manage the endpoints in EnpointHolder to keep balance;
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-8
 */

public interface EndpointManager<R> {

	void init(EndpointPool<R> endpointPool, EndpointBalancerConfig config);
	
//	List<String> getOrderedIpAddresses();
	
	public EndpointBalancerConfig getConfig();

}
