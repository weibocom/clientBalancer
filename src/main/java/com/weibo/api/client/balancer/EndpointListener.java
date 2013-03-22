package com.weibo.api.client.balancer;
/**
 * 
 * Listening endpint lifecycle: create, destroy
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-10
 */

public interface EndpointListener<R> {

	void onEndpointCreate(Endpoint<R> endpoint);
	
	void onEndpointDestroy(Endpoint<R> endpoint);
}
