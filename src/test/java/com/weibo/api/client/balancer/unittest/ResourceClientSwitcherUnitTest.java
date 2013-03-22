package com.weibo.api.client.balancer.unittest;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.weibo.api.client.balancer.Endpoint;
import com.weibo.api.client.balancer.EndpointBalancerException;
import com.weibo.api.client.balancer.ResourceClientSwitcher;
import com.weibo.api.client.balancer.impl.EndpointBalancerConfig;
import com.weibo.api.client.balancer.impl.EndpointFactory;
import com.weibo.api.client.balancer.impl.EndpointPoolImpl;

/**
 * 
 * ClientSwitcher UnitTest
 *
 * @author fishermen
 * @version V1.0 created at: 2012-9-12
 */

public class ResourceClientSwitcherUnitTest extends BaseTest{

	private EndpointPoolImpl<Object> endpointPool;
	
	@Before
	public void setup() {
		EndpointFactory<Object> endpointFactory = new EndpointFactory<Object>(config) {
		
			@Override
			public com.weibo.api.client.balancer.Endpoint<Object> doCreateEndpoint(String ip, EndpointBalancerConfig config) throws Exception {
				return new Endpoint<Object>(new Object(), ipAddresses.get(0), port);
			}
			
			@Override
			public Endpoint<Object> doDestroyEndpoint(Endpoint<Object> endpoint) {
				endpoint.createdTime.set(-1);
				return endpoint;
			}
			@Override
			public boolean doValidateEndpoint(Endpoint<Object> endpoint) {
				return true;
			}
		};
		endpointPool = new EndpointPoolImpl<Object>(endpointFactory);
	}
	
	@Test
	public void testPauseService(){
		Endpoint<Object> ep = endpointPool.borrowEndpoint();
		endpointPool.returnEndpoint(ep);
		Assert.assertNotNull(ep);
		
		ResourceClientSwitcher.pauseService(endpointPool.getConfig().getHostname(), endpointPool.getConfig().getPort());
		
		try {
			ep = endpointPool.borrowEndpoint();
		} catch (EndpointBalancerException e) {
			ep = null;
		}
		Assert.assertNull(ep);
	}
	
	@Test
	public void testRestartService(){
		testPauseService();
		
		ResourceClientSwitcher.restartService(endpointPool.getConfig().getHostname(), endpointPool.getConfig().getPort());
		Endpoint<Object> ep = endpointPool.borrowEndpoint();
		Assert.assertNotNull(ep);
	}
}
