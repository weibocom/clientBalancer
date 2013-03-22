package com.weibo.api.client.balancer.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool.BasePoolableObjectFactory;

import com.weibo.api.client.balancer.Endpoint;
import com.weibo.api.client.balancer.EndpointListener;
import com.weibo.api.client.balancer.HostAddressListener;
import com.weibo.api.client.balancer.util.ClientBalancerLog;
import com.weibo.api.client.balancer.util.ClientBalancerUtil;

/**
 * 
 * A implementation of BasePoolableObjectFactory, for creating, destroying, validating endpoint.
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-8
 */

public abstract class EndpointFactory<R> extends BasePoolableObjectFactory<Endpoint<R>> implements HostAddressListener{

	protected EndpointBalancerConfig config;
	
	protected List<String> ipAddresses = new CopyOnWriteArrayList<String>();
	
	private AtomicInteger dispatchCounter = new AtomicInteger(0);
	
	private static int MAX_IP_DISPATCH_COUNTER = 10000; 
	
	private CopyOnWriteArrayList<EndpointListener<R>> endpointListeners = new CopyOnWriteArrayList<EndpointListener<R>>();
	
	public EndpointFactory(EndpointBalancerConfig config){
		this.config = config;
		
		Set<String> ips = ClientBalancerUtil.getAllIps(this.config.getHostname());
		
		if (ips == null || ips.isEmpty()) {
			this.ipAddresses.add(this.config.getHostname());
		} else {
			clearAndAddIps(ips);
		}
	}
	
	public void addListener(EndpointListener<R> listener){
		endpointListeners.add(listener);
	}
	
    /**
     * {@inheritDoc}
     */
	@Override
    public final Endpoint<R> makeObject() throws Exception{
		Endpoint<R> endpoint = null;
		int tryCount = getHostAddressesCount();
		while(tryCount-- > 0){
			String ip = getNextIp();
			try {
				endpoint = doCreateEndpoint(ip, config);
				if(endpoint != null){
					break;
				}
			} catch (Exception e) {
				ClientBalancerLog.log.warn("Create Endpoint false ip:" + ip, e);
			}
		}
		
		if(endpoint == null){
			ClientBalancerLog.log.warn("create endpoint false for {}", this.config.getHostnamePort());
			throw new NullPointerException("makeObject: create endpoint false for {" + this.config.getHostnamePort() + "}"); 
		}
		for(EndpointListener<R> listener : endpointListeners){
			listener.onEndpointCreate(endpoint);
		}
    	
    	
    	ClientBalancerLog.log.info("create endpoint completed! {}", endpoint);
    	return endpoint;
    }
	
	/**
	 * crate
	 * @return
	 */
	public abstract Endpoint<R> doCreateEndpoint(String ip, EndpointBalancerConfig config) throws Exception;

	/**
     * {@inheritDoc}
     */
	@Override
    public final void destroyObject(Endpoint<R> endpoint)
        throws Exception  {
		if (endpoint == null) {
			ClientBalancerLog.log.warn("destroyObject endpoint is null {" + this.config.getHostnamePort() + "}");
			return;
		}
		
		doDestroyEndpoint(endpoint);
		
		for(EndpointListener<R> listner : endpointListeners){
			listner.onEndpointDestroy(endpoint);
		}
		
		ClientBalancerLog.log.info("destroy endpoint {}", endpoint);
    }
	
	public abstract Endpoint<R> doDestroyEndpoint(Endpoint<R> endpoint);

	/**
     * {@inheritDoc}
     */
	@Override
    public final boolean validateObject(Endpoint<R> endpoint) {
		if (endpoint == null) {
			ClientBalancerLog.log.warn("validateObject endpoint is null {" + this.config.getHostnamePort() + "}");
			return false;
		}
		
		ClientBalancerLog.log.debug("validateObject ep={}", endpoint);
		return doValidateEndpoint(endpoint);
    }
	
	public abstract boolean doValidateEndpoint(Endpoint<R> endpoint);

	/**
     * {@inheritDoc}
     */
	@Override
    public final void activateObject(Endpoint<R> endpoint) throws Exception {
		if (endpoint == null) {
			throw new NullPointerException("activateObject: endpoint is null {" + this.config.getHostnamePort() + "}");
		}
    }

	/**
     * {@inheritDoc}
     */
	@Override
    public final void passivateObject(Endpoint<R> endpoint)
        throws Exception {
		//do nothing!
		//ClientBalancerLog.log.warn("EndpintFactory does not support passivateObject!");
    }
	
	@Override
	public void onHostAddressChanged(String hostname, Set<String> latestIps) {
		ClientBalancerLog.log.info("EndpintFactory - onHostAddressChanged get notify, hostname={}, newIps={}", hostname, latestIps);
		synchronized (ipAddresses) {
			clearAndAddIps(latestIps);
		}
		ClientBalancerLog.log.info("EndpintFactory - onHostAddressChanged after refresh, now in factory hostname={}, ipAddresses={}", hostname, this.ipAddresses);
	}
	
	/**
	 * clear current ip list and add new ip list
	 * 
	 * @param latestIps
	 */
	protected void clearAndAddIps (Set<String> latestIps) {
		this.ipAddresses.clear();

		List<String> temp = new ArrayList<String>(latestIps);
		
		int random = new Random().nextInt(temp.size());
		
		for (int i = 0; i < temp.size(); i ++) {
			int index = (i + random) % temp.size(); 
			this.ipAddresses.add(temp.get(index));
		}
	}
	
	/**
	 * get config
	 * @return
	 */
	public EndpointBalancerConfig getConfig() {
		return config;
	}

	private String getNextIp(){
		int dcount = dispatchCounter.addAndGet(1);
		
		String nextIp = null;
		synchronized(ipAddresses){
			nextIp = this.ipAddresses.get(dcount % ipAddresses.size());
		}

		if(dcount > MAX_IP_DISPATCH_COUNTER){
			synchronized (dispatchCounter) {
				if(dispatchCounter.get() > MAX_IP_DISPATCH_COUNTER){
					dispatchCounter.set(0);
				}
			}
		}
		
		ClientBalancerLog.log.info("getNextIp for hostname {} is {}, idx={}", new Object[]{this.config.getHostname(), nextIp, dcount});
		return nextIp;
	}
	
	private int getHostAddressesCount(){
		synchronized (ipAddresses) {
			return this.ipAddresses.size();
		}
	}

	public List<String> getIpAddresses() {
		return ipAddresses;
	}
}
