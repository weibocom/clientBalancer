package com.weibo.api.client.balancer.unittest;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.weibo.api.client.balancer.Endpoint;
import com.weibo.api.client.balancer.impl.EndpointHolderImpl;

/**
 * 
 * EndpointHolderImpl unit test
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-14
 */

public class EndpointHolderImplUnitTest extends BaseTest{

	private EndpointHolderImpl<Object> endpointHolder;
	
	@Before
	public void setup(){
		endpointHolder = new EndpointHolderImpl<Object>(hostname, port);
	}
	
	@Test
	public void getEndpointCounter(){
		Map<String, Integer> ipCounter = endpointHolder.getEndpointCounter();
		Assert.assertTrue(ipCounter.size() == 0);
	}
	
	@Test
	public void addEndpoint(){
		int count1 = getEnpointCounter(ipAddresses.get(0));
		
		Endpoint<Object> endpoint = mockEndpoint(ipAddresses.get(0));
		endpointHolder.addEndpoint(endpoint);
		
		int count2 = getEnpointCounter(ipAddresses.get(0));
		
		Assert.assertTrue(count2 == (count1 + 1));
	}
	
	@Test
	public void removeEndpoint(){
		int c1 = getEnpointCounter(ipAddresses.get(0));
		
		Endpoint<Object> ep1 = mockEndpoint();
		Endpoint<Object> ep2 = mockEndpoint();
		endpointHolder.addEndpoint(ep1);
		endpointHolder.addEndpoint(ep2);
		endpointHolder.removeEndpoint(ep1);
		
		int c2 = getEnpointCounter(ipAddresses.get(0));
		
		Assert.assertTrue(c2 == (c1 + 1));
	}
	
	@Test
	public void getHostname(){
		Assert.assertEquals(hostname, endpointHolder.getHostname());
	}
	
	@Test
	public void getPort(){
		Assert.assertTrue(port == endpointHolder.getPort());
	}
	
	@Test
	public void getEndpoints(){
		int size1 = endpointHolder.getEndpoints(ipAddresses.get(0)).size();
		
		endpointHolder.addEndpoint(mockEndpoint());
		endpointHolder.addEndpoint(mockEndpoint());
		
		int size2 = endpointHolder.getEndpoints(ipAddresses.get(0)).size();
		Assert.assertTrue(size2 == (size1 + 2));
	}
	
	private int getEnpointCounter(String ip){
		Integer count = endpointHolder.getEndpointCounter().get(ip);
		return count == null ? 0 : count.intValue();
	}
	
}
