package com.weibo.api.client.balancer.unittest;

import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.junit.Test;

import com.weibo.api.client.balancer.HostAddressListener;
import com.weibo.api.client.balancer.HostAddressWatcher;
import com.weibo.api.client.balancer.impl.HostAddressWatcherImpl;
import com.weibo.api.client.balancer.util.ClientBalancerConstant;
import com.weibo.api.client.balancer.util.ClientBalancerUtil;

/**
 * 
 * 类说明
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-15
 */

public class HostAddressWatcherUnitTest extends BaseTest{

	private HostAddressWatcher hostAddressWatcher = HostAddressWatcherImpl.getInstance();
	
	@Test
	public void registerAndWatch(){
		final HostAddressListener listener = mockery.mock(HostAddressListener.class);
		mockery.checking(new Expectations(){{
			one(listener).onHostAddressChanged(hostname, new HashSet<String>(ipAddresses));
		}});
		hostAddressWatcher.register(hostname, listener);
		hostAddressWatcher.tryWatchHostAddressImmediately(hostname);
	}
	
	public static void main(String[] args){
		final HostAddressListener listener = new HostAddressListener() {
			@Override
			public void onHostAddressChanged(String hostname, Set<String> latestIps) {
				System.out.println("Dns changed, hostname:" + hostname);
			}
		};
		HostAddressWatcherImpl.getInstance().register("s248", listener);
		ClientBalancerUtil.safeSleep(130 * 1000);
		ClientBalancerConstant.DEFAULT_HOST_ADDRESS_WATCH_INTERVAL = 5;
		
	}
}
