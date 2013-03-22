package com.weibo.api.client.balancer.unittest;

import junit.framework.Assert;

import org.junit.Test;

import com.weibo.api.client.balancer.Endpoint;

/**
 * 
 * endpoint unitTest
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-10
 */

public class EndpointUnitTest {

	public void testToString(){
		String ip = "192.168.11.248";
		int port = 8888;
		
		Endpoint<Object> ep = new Endpoint<Object>(new Object(), ip, port);
		Assert.assertTrue(ep.toString().contains(ip));
	}
	
	@Test
	public void testEquals(){
		String ip = "192.168.11.248";
		int port = 8888;
		
		Endpoint<Object> ep = new Endpoint<Object>(new Object(), ip, port);
		Endpoint<Object> ep2 = new Endpoint<Object>(new Object(), ip, port);
		
		Assert.assertTrue(!ep.equals(ep2));
	}
}
