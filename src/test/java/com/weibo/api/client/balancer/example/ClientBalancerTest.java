package com.weibo.api.client.balancer.example;

import java.util.ArrayList;
import java.util.List;

import com.weibo.api.client.balancer.Endpoint;
import com.weibo.api.client.balancer.EndpointBalancerException;
import com.weibo.api.client.balancer.EndpointPool;
import com.weibo.api.client.balancer.impl.EndpointBalancerConfig;
import com.weibo.api.client.balancer.impl.EndpointFactory;
import com.weibo.api.client.balancer.impl.EndpointPoolImpl;

/**
 * 
 * ClientBanalcer test
 *
 * @author fishermen
 * @version V1.0 created at: 2012-9-21
 */

public class ClientBalancerTest {

	private String hostname = "s248";
	private int port = 8000;
	private int minPoolSize = 2;
	private int maxPoolSize = 10;
	
	private List<String> addresses = new ArrayList<String>();
	private EndpointPool<Object> endpointPool;
	private EndpointBalancerConfig config;
	
	public static void main(String[] args){
		ClientBalancerTest test = new ClientBalancerTest();
		test.init();
		test.doTest();
	}
	
	public void init(){
		config = new EndpointBalancerConfig();
		config.setHostname(hostname);
		config.setPort(port);
		config.setMinPoolSize(minPoolSize);
		config.setMaxPoolSize(maxPoolSize);
		
		addresses.add("192.168.11.248");
		addresses.add("192.168.11.249");
		
		EndpointFactory<Object> endpointFactory = new EndpointFactory<Object>(config) {
			
			@Override
			public com.weibo.api.client.balancer.Endpoint<Object> doCreateEndpoint(String ip, EndpointBalancerConfig config) throws Exception {
				return new Endpoint<Object>(new Object(), addresses.get(0), port);
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
	
	public void doTest(){
		Endpoint<Object> client = null;
		boolean closeClient = false;
		try {
			client = endpointPool.borrowEndpoint();
			int hash = client.resourceClient.hashCode();
			System.out.println("client.hash=" + hash);
		} catch (EndpointBalancerException e) {
			closeClient = true;
		}finally{
			if(!closeClient){
				endpointPool.returnEndpoint(client);
			}else{
				endpointPool.invalidateEndpoint(client);
			}
		}
	}
}
