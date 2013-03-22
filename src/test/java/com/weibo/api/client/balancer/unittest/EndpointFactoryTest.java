package com.weibo.api.client.balancer.unittest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.weibo.api.client.balancer.Endpoint;
import com.weibo.api.client.balancer.impl.EndpointBalancerConfig;
import com.weibo.api.client.balancer.impl.EndpointFactory;

public class EndpointFactoryTest extends BaseTest {

	private String hostname = "test_test.test.test.weibo.com";
	private EndpointBalancerConfig config = new EndpointBalancerConfig();
	{
		config.setHostname(hostname);
	}

	@Test
	public void getAllIps() {
		EndpointFactory<Set<String>> factory = new MockEndpointFactory(config);

		Assert.assertEquals(factory.getIpAddresses().get(0), hostname);
	}

	@Test
	public void clearAndAddIps() {
		MockEndpointFactory factory = new MockEndpointFactory(config);

		List<String> result = factory.getIpAddresses();

		Assert.assertEquals(result.size(), 1);
		
		HashSet<String> ips = initLatestIps();

		// 测试用例不关注随机性，只关注数据一致 
		for (int i = 0; i < 10; i++) {
			factory.clearAndAddIps(ips);

			result = factory.getIpAddresses();

			Assert.assertEquals(result.size(), ips.size());
			
			for (String ip : ips) {
				result.remove(ip);
			}

			Assert.assertEquals(result.size(), 0);
		}

	}

	private HashSet<String> initLatestIps() {
		HashSet<String> latestIps = new HashSet<String>();
		latestIps.add("A");
		latestIps.add("B");
		latestIps.add("C");
		return latestIps;
	}

	static class MockEndpointFactory extends EndpointFactory<Set<String>> {

		public MockEndpointFactory(EndpointBalancerConfig config) {
			super(config);
		}

		@Override
		public void onHostAddressChanged(String hostname, Set<String> latestIps) {
		}

		@Override
		public Endpoint<Set<String>> doCreateEndpoint(String ip, EndpointBalancerConfig config) throws Exception {
			return null;
		}

		@Override
		public Endpoint<Set<String>> doDestroyEndpoint(Endpoint<Set<String>> endpoint) {
			return null;
		}

		@Override
		public boolean doValidateEndpoint(Endpoint<Set<String>> endpoint) {
			return false;
		}
		
		protected void clearAndAddIps (Set<String> latestIps) {
			super.clearAndAddIps(latestIps);
		}
	};
}
