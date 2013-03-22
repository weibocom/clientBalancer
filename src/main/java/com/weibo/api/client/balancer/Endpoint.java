package com.weibo.api.client.balancer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.weibo.api.client.balancer.util.SystemTimer;

/**
 * 
 * Endpoint of the resource,
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-8
 */

public class Endpoint<R> {

	public final int id;
	public final String ipAddress;
	public final int port;
	public final R resourceClient;
	
	public final AtomicLong createdTime = new AtomicLong(System.currentTimeMillis());
	public final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());
	
	private static AtomicInteger endpointIdCreator = new AtomicInteger(0);
	
	public Endpoint(R resourceClient, String ipAddress, int port){
		this.id = endpointIdCreator.addAndGet(1);
		
		this.resourceClient = resourceClient;
		this.ipAddress = ipAddress;
		this.port = port;
	}
	
	public void refreshLastAccessTime(){
		lastAccessTime.set(SystemTimer.currentTimeMillis());
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(64)
			.append("[id=").append(id)
			.append(", ip=").append(ipAddress)
			.append(", port=").append(port)
			.append(", resourceClient=").append(resourceClient);
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		return id + 7 * ipAddress.hashCode() + 17 * port + 117 * resourceClient.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj){
			return true;
		}
		
		if(obj == null || !(obj instanceof Endpoint)){
			return false;
		}
		
		final Endpoint<R> epObj = (Endpoint)obj;
		return this.id == epObj.id
				&& this.ipAddress.equals(epObj.ipAddress)
				&& this.port == epObj.port
				&& this.resourceClient.equals(epObj.resourceClient);
	}
}
