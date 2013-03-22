package com.weibo.api.client.balancer.unittest;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.client.balancer.Endpoint;
import com.weibo.api.client.balancer.impl.EndpointBalancerConfig;

/**
 * 
 * Base test for other test
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-10
 */

public class BaseTest {

	protected final String hostname = "i.api.weibo.com";
	protected final int port = 9999;
	protected final List<String> ipAddresses = new ArrayList<String>();
	protected final EndpointBalancerConfig config = new EndpointBalancerConfig();
	
	protected JUnit4Mockery mockery = new JUnit4Mockery(){{
		setImposteriser(ClassImposteriser.INSTANCE);
	}};
	
	@Before
	public void init(){
		config.setHostname(hostname);
		config.setPort(port);
		
		//第一个ip改为dns实际解析的ip，用于测试
		String ip1 = "192.168.11.248";
		String ip2 = "192.168.11.249";
		String ip3 = "192.168.11.33";
		String ip4 = "192.168.11.34";
		ipAddresses.add(ip1);
		ipAddresses.add(ip2);
		ipAddresses.add(ip3);
		ipAddresses.add(ip4);
	}
	
	@Test
	public void testCheck(){
		Assert.assertNotNull(mockery);
	}
	
	protected Endpoint<Object> mockEndpoint(String ipAddress){
		return new Endpoint<Object>(new Object(), ipAddress, port);
	}
	
	protected Endpoint<Object> mockEndpoint(){
		return mockEndpoint(ipAddresses.get(0));
	}
	
}
