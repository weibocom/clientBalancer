package com.weibo.api.client.balancer.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.weibo.api.client.balancer.Endpoint;
import com.weibo.api.client.balancer.EndpointHolder;
import com.weibo.api.client.balancer.util.ClientBalancerLog;

/**
 * 
 * Endpoint holder, keeps the counter of the ip connection;
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-8
 */

public class EndpointHolderImpl<R> implements EndpointHolder<R>{
	
	private Map<String, Set<Endpoint<R>>> ipEndpoints = new HashMap<String, Set<Endpoint<R>>>();
	
	private String hostname;
	
	private int port;
	
	public EndpointHolderImpl(String hostname, int port){
		this.hostname = hostname;
		this.port = port;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Integer> getEndpointCounter(){
		Map<String, Integer> ipEndpointCounter = new HashMap<String, Integer>();
		synchronized (ipEndpoints) {
			for(Map.Entry<String, Set<Endpoint<R>>> entry : ipEndpoints.entrySet()){
				ipEndpointCounter.put(entry.getKey(), entry.getValue().size());
			}
		}
		
		return ipEndpointCounter;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addEndpoint(Endpoint<R> endpoint){
		synchronized (ipEndpoints) {
			Set<Endpoint<R>> endpointsOfSameServer = ipEndpoints.get(endpoint.ipAddress);
			if(endpointsOfSameServer == null){
				endpointsOfSameServer = new HashSet<Endpoint<R>>();
				ipEndpoints.put(endpoint.ipAddress, endpointsOfSameServer);
			}
			endpointsOfSameServer.add(endpoint);
		}
		ClientBalancerLog.log.debug("EndpontHolder - add endpoint {}, now epHolder {}", endpoint, this);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeEndpoint(Endpoint<R> endpoint){
		synchronized (ipEndpoints) {
			Set<Endpoint<R>> endpointsOfSameServer = ipEndpoints.get(endpoint.ipAddress);
			if(endpointsOfSameServer != null){
				endpointsOfSameServer.remove(endpoint);
				if(endpointsOfSameServer.size() == 0){
					ipEndpoints.remove(endpoint.ipAddress);
				}
			}
		}
		ClientBalancerLog.log.debug("EndpontHolder - remove endpoint {}, now epHolder {}", endpoint, this);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Set<Endpoint<R>> getEndpoints(String ipAddress) {
		Set<Endpoint<R>> eps = ipEndpoints.get(ipAddress);
		if(eps != null){
			return eps;
		}
		return Collections.EMPTY_SET;
	}
	
	@Override
	public String getHostname() {
		return hostname;
	}
	
	@Override
	public int getPort() {
		return port;
	}
	
	@Override
	public String toString() {
		return new StringBuilder(64)
			.append("endpoinCount=").append(getEndpointCounter()).toString();
	}
	
}
