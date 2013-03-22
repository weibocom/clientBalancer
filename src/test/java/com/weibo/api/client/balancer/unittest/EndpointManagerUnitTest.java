package com.weibo.api.client.balancer.unittest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.client.balancer.Endpoint;
import com.weibo.api.client.balancer.EndpointPool;
import com.weibo.api.client.balancer.impl.EndpointManagerImpl;

/**
 * 
 * EndpointManagerImpl unit test
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-14
 */

public class EndpointManagerUnitTest extends BaseTest{

	private final EndpointManagerImpl<Object> endpointManager = new EndpointManagerImpl<Object>();
	
	private EndpointPool<Object> endpointPool = mockery.mock(EndpointPool.class);
	
	private List<Endpoint<Object>> endpoints = new ArrayList<Endpoint<Object>>();
	
	@Before
	public void setup(){
		
		endpointManager.init(endpointPool, config);
		
		//init endpoints
		endpoints.add(mockEndpoint(ipAddresses.get(1)));
		endpoints.add(mockEndpoint(ipAddresses.get(2)));
		endpoints.add(mockEndpoint(ipAddresses.get(2)));
		endpoints.add(mockEndpoint(ipAddresses.get(2)));
		endpoints.add(mockEndpoint(ipAddresses.get(3)));
		endpoints.add(mockEndpoint(ipAddresses.get(3)));
		
		for(Endpoint<Object> ep : endpoints){
			endpointManager.onEndpointCreate(ep);
		}
	}
	
//	@Test
//	public void getOrderedIpAddresses(){
//		
//		List<String> orderedIps = endpointManager.getOrderedIpAddresses();
//		
//		Assert.assertEquals(orderedIps.get(0), ipAddresses.get(0));
//		Assert.assertEquals(orderedIps.size(), 1);
//	}
	
	@Test
	public void onHostAddressChanged(){
		mockery.checking(new Expectations(){{
			atLeast(1).of(endpointPool).removeOfflineIdleEndpoints(with(equal(new HashSet<String>(ipAddresses.subList(0, 1)))));
			oneOf(endpointPool).removeOfflineEndpointsCompleted();
		}});
		for(Endpoint<Object> ep : endpoints){
			endpointManager.onEndpointDestroy(ep);
		}
		endpointManager.onHostAddressChanged(hostname, new HashSet<String>(ipAddresses));
	}
	
	@Test
	public void onEndpointCreate(){
		endpointManager.onEndpointCreate(mockEndpoint(ipAddresses.get(2)));
	}
	
	@Test
	public void onEndpointDestroy(){
		endpointManager.onEndpointDestroy(mockEndpoint(ipAddresses.get(2)));
	}
}
