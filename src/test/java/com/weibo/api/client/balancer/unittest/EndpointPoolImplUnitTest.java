package com.weibo.api.client.balancer.unittest;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.client.balancer.Endpoint;
import com.weibo.api.client.balancer.impl.EndpointBalancerConfig;
import com.weibo.api.client.balancer.impl.EndpointFactory;
import com.weibo.api.client.balancer.impl.EndpointPoolImpl;

/**
 * 
 * 类说明
 * 
 * @author fishermen
 * @version V1.0 created at: 2012-8-15
 */

public class EndpointPoolImplUnitTest extends BaseTest {

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
	public void borrowEndpoint() throws Exception {

		clearEndpointsPool(new HashSet<String>(ipAddresses));
		endpointPool.addEndpoint();
		Endpoint<Object> epInPool = endpointPool.borrowEndpoint();

		Assert.assertNotNull(epInPool);
	}
	
	@Test
	public void testGetConn () {
		Assert.assertEquals("Conn<REDIS " + config.getHostnamePort(), endpointPool.getPoolName());
	}
	
	@Test
	public void returnEndpoint() {
		Endpoint<Object> endpoint = mockEndpoint();
		int idleCount1 = endpointPool.getNumIdle();
		endpointPool.returnEndpoint(endpoint);
		int idleCount2 = endpointPool.getNumIdle();

		Assert.assertEquals(idleCount2, (idleCount1 + 1));
	}

	@Test
	public void invalidateEndpoint() throws Exception {
		clearEndpointsPool(new HashSet<String>(ipAddresses));
		endpointPool.addEndpoint();
		Endpoint<Object> epInPool = endpointPool.borrowEndpoint();
		endpointPool.invalidateEndpoint(epInPool);

		Assert.assertEquals(epInPool.createdTime.get(), -1);
	}

	@Test
	public void invalidateEndpoints() throws Exception {

		endpointPool.addEndpoint();
		Endpoint<Object> endpoint = endpointPool.borrowEndpoint();
		endpointPool.returnEndpoint(endpoint);
		
		Set<String> reservedIps = new HashSet<String>();
		reservedIps.add("192.168.43.71");
		clearEndpointsPool(reservedIps);

		Assert.assertEquals(endpoint.createdTime.get(), -1);
	}

	@Test
	public void addEndpoint() throws Exception {
		endpointPool.addEndpoint();
		Endpoint<Object> endpoint = endpointPool.borrowEndpoint();
		endpointPool.returnEndpoint(endpoint);
		Assert.assertNotNull(endpoint);
	}

	@Test
	public void getNumIdle() throws Exception {
		int idleNum1 = endpointPool.getNumIdle();

		endpointPool.addEndpoint();

		int idleNum2 = endpointPool.getNumIdle();

		Assert.assertEquals(idleNum2, (idleNum1 + 1));
	}

	@Test
	public void getNumActive() throws Exception {
		int activeNum1 = endpointPool.getNumActive();

		endpointPool.addEndpoint();
		endpointPool.borrowEndpoint();

		int activeNum2 = endpointPool.getNumActive();

		Assert.assertEquals(activeNum2, (activeNum1 + 1));

	}
	
	private void clearEndpointsPool(Set<String> reservedIps){
		endpointPool.removeOfflineIdleEndpoints(new HashSet<String>(reservedIps));
		endpointPool.removeOfflineEndpointsCompleted();
	}
}
