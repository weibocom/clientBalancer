package com.weibo.api.client.balancer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.weibo.api.client.balancer.Endpoint;
import com.weibo.api.client.balancer.EndpointHolder;
import com.weibo.api.client.balancer.EndpointListener;
import com.weibo.api.client.balancer.EndpointManager;
import com.weibo.api.client.balancer.EndpointPool;
import com.weibo.api.client.balancer.HostAddressListener;
import com.weibo.api.client.balancer.util.ClientBalancerLog;
import com.weibo.api.client.balancer.util.ClientBalancerUtil;

/**
 * 
 * Refreshes clients when dns changed, keep number balancer for the pool.
 * 
 * @author fishermen
 * @version V1.0 created at: 2012-8-8
 */

public class EndpointManagerImpl<R> implements EndpointManager<R>, HostAddressListener, EndpointListener<R>{
	
	private EndpointPool<R> endpointPool;
	private EndpointBalancerConfig config;
	
	private EndpointHolder<R> endpointHolder;
	
	//use for keep endpoints in pool to be balance.
	private ScheduledExecutorService scheduledService = Executors.newScheduledThreadPool(1);
	
	private static int REFRESH_CONFIRM_COUNT = 10;
	private static int REFRESH_INTERVAL_WHEN_IP_CHANGED = 30 * 1000;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void init(EndpointPool<R> endpointPool, EndpointBalancerConfig config) {
		if(endpointHolder != null){
			ClientBalancerLog.log.warn("Duplicate init the endpoinManager, hostname:port={}:{}", config.getHostname(), config.getPort());
			throw new UnsupportedOperationException(String.format("Duplicate init the endpoinManager, hostname:port=%s:%s", config.getHostname(), config.getPort()));
		}
		
		this.endpointPool = endpointPool;
		this.endpointHolder = new EndpointHolderImpl<R>(config.getHostname(), config.getPort());
		this.config = config;
		
		startWatchPool();
	}
	
	@Override
	public void onEndpointCreate(Endpoint<R> endpoint) {
		endpointHolder.addEndpoint(endpoint);
	}
	
	@Override
	public void onEndpointDestroy(Endpoint<R> endpoint) {
		endpointHolder.removeEndpoint(endpoint);
	}

	/**
	 * Reset hostAddresses, and close endpoints with abandon ip
	 */
	@Override
	public void onHostAddressChanged(final String hostname, java.util.Set<String> newIps) {
		ClientBalancerLog.log.warn("EndpintManager - onHostAddressChanged, hostname={}, newIps={}", hostname, newIps);
		
		//用独立线程来刷新连接，清理掉下线ip的client
		//不共用scheduledService的原因：此处是独立任务，刷新完毕后就退出，但一次刷新的时间不定
		//由于dns的变更时间不定，可能存在短时频繁更新的情况，因此每次refresh时，需要使用最新的ips列表
		/////////////////////////////////////////////////////////////////////////
		Thread refreshEpPoolThread = new Thread(){
			@Override
			public void run() {
				try {
					refreshEndpointPool(hostname);
				} catch (Exception e) {
					ClientBalancerLog.log.error("Error: when refresh endpoints for " + hostname, e);
				}
			}
		};
		refreshEpPoolThread.start();
	}

	public EndpointBalancerConfig getConfig() {
		return config;
	}
	
	private void startWatchPool(){
		ClientBalancerLog.log.debug("StartWatchPoolHealthy, endpointHolder={}.", endpointHolder);
		scheduledService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				checkHealthy();
			}
		}, ClientBalancerUtil.next(config.getPoolHealthyInterval()), config.getPoolHealthyInterval(), TimeUnit.MILLISECONDS);
		
		scheduledService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				watchPool();
			}
		}, ClientBalancerUtil.next(config.getPoolWatchInterval()), config.getPoolWatchInterval(), TimeUnit.MILLISECONDS);
	}
	
	private void checkHealthy() {
		//watch if healthy
		try {
			endpointPool.doCheckHealthy();			
		} catch (Exception e) {
			ClientBalancerLog.log.warn("Warn: meet exception when doKeepEndpoinsInPoolBalance", e);
		}
	}
	
	private void watchPool(){
		//watch if idle
		try {
			int retryCount = this.config.getMinPoolSize();
			while(retryCount-- > 0){
				tryRemoveOneIdleEndpoint(endpointPool.isIdle());
			}
			ClientBalancerLog.log.info("After watch pool balancer & idle completed, endpoinHolder:{}, epPool:{}", endpointHolder, endpointPool);
		} catch (Exception e) {
			ClientBalancerLog.log.warn("Warn: meet exception when doKeepEndpoinsInPoolBalance", e);
		}
	}
	
	private void tryRemoveOneIdleEndpoint(boolean forceRemove){
		
		Map<String, Integer> ipEndpointCounter = endpointHolder.getEndpointCounter();
		if(ipEndpointCounter.size() == 0){
			return;
		}
		
		List<NameValue> ipCounters = new ArrayList<EndpointManagerImpl.NameValue>();
		for(Map.Entry<String, Integer> entry : ipEndpointCounter.entrySet()){
			ipCounters.add(new NameValue(entry.getKey(), entry.getValue()));
		}
		sortIpCounters(ipCounters);
		
		//minCounter 要大于0，避免某个ip无法建立建立而导致误清理
		int minCounter = ipCounters.get(0).value;
		if(minCounter < 1){
			for(NameValue ipc : ipCounters){
				if(ipc.value > 0){
					minCounter = ipc.value;
					break;
				}
			}
		}
		
		boolean isBalance = (ipCounters.get(ipCounters.size() - 1).value - minCounter) <= 1;
		if(forceRemove || !isBalance){
			Endpoint<R> removedEp = endpointPool.tryInvalidateOneIdleEndpoint(ipCounters.get(ipCounters.size() - 1).name);
			ClientBalancerLog.log.info("Removed ep={}, before removed epHolder {}, after remove epPool={}, isBalance={}", new Object[]{removedEp, ipEndpointCounter, endpointPool, isBalance});
		}
	}

	private static class NameValue{
		public final String name;
		public final int value;
		
		public NameValue(String name, int value){
			this.name = name;
			this.value = value;
		}
		
		@Override
		public String toString() {
			return new StringBuilder(64)
				.append("[ip=").append(name)
				.append(", allEps=").append(value)
				.append("]").toString();
		}
	}
	
	private static void sortIpCounters(List<NameValue> ipCounters){
		Collections.sort(ipCounters, new Comparator<NameValue>() {
			public int compare(NameValue o1, NameValue o2) {
				return o1.value - o2.value;
			};
		});
	}
	
	/**
	 * 刷新endpointsPool，去掉下线的endpoint，由于dns可能会连续变更，而refresh需要重复多次，因此每次refresh时需要使用最新的ip列表
	 * 连续REFRESH_CONFIRM_COUNT次在pool中发现不再有下线的ip时，则认为已经完全清理干净
	 * 需要多次确认的原因是：此处检查时，可能并发创建了下线ip的client
	 * @param hostname
	 */
	private void refreshEndpointPool(String hostname){
		ClientBalancerLog.log.info("Refresh endpoints - start epHolder={}", new Object[]{endpointHolder});
		int refreshSuccessTime = 0;
		while(refreshSuccessTime < REFRESH_CONFIRM_COUNT){
			Set<String> newIpAddresses = null;
			try {
				newIpAddresses = ClientBalancerUtil.getAllIps(hostname);
				if(newIpAddresses == null || newIpAddresses.size() == 0){
					ClientBalancerLog.log.info("Refresh pool false for getAllIps failure, hostname={}", hostname);
					continue;
				}
				Map<String, Integer> ipEndpointCounter = endpointHolder.getEndpointCounter();
				Set<String> abandonedIps = new HashSet<String>(ipEndpointCounter.keySet());
				abandonedIps.removeAll(newIpAddresses);
				
				if(abandonedIps.size() > 0){
					endpointPool.removeOfflineIdleEndpoints(newIpAddresses);
					ClientBalancerLog.log.info("Refresh endpoints - found abandon ips in epHolder, latest ips={}, endpointHolder={}, abandonedIps={}", new Object[]{newIpAddresses, endpointHolder, abandonedIps});
				}else{
					refreshSuccessTime++;
					//如果所有ip都是合法的ip，但epHolder中ip比newIpAddresses少，则清理掉若干连接，来让新ip尽快入pool
					if(newIpAddresses.size() > ipEndpointCounter.size()){
						for(int i = 0; i < newIpAddresses.size(); i++){
							tryRemoveOneIdleEndpoint(true);
						}
						ClientBalancerLog.log.info("Refresh endpoints - not found abandon ips in epHolder, but remove some idle endpoints latest ips={}, befor remove endpointHolder={}, after remove epPool={}", new Object[]{newIpAddresses, endpointHolder, endpointPool});
					}else{
						ClientBalancerLog.log.info("Refresh endpoints - not found abandon ips in epHolder, latest ips={}, endpointHolder={}, abandonedIps={}", new Object[]{newIpAddresses, endpointHolder, abandonedIps});
					}
					
					
				}
			} catch (Exception e) {
				ClientBalancerLog.log.error("Refresh endpoints - Error: when refresh endpointPool for ipAddress changed! newIpAddress=" + newIpAddresses, e);
			}finally{
				ClientBalancerLog.log.info("Refresh endpoints - one loop completed! loop count={}, now epHolder={}, epPool={}, will sleep a momment", new Object[]{refreshSuccessTime, endpointHolder, endpointPool});
				ClientBalancerUtil.safeSleep(REFRESH_INTERVAL_WHEN_IP_CHANGED);
			}
		}
		endpointPool.removeOfflineEndpointsCompleted();
		ClientBalancerLog.log.info("Refresh endpoints - all completed! loop count={}, now epHolder={} epPool={}", new Object[]{refreshSuccessTime, endpointHolder, endpointPool});
	}
	
	public static void main(String[] args){
		List<NameValue> nameValues = new ArrayList<EndpointManagerImpl.NameValue>();
		nameValues.add(new NameValue("name1", 2));
		nameValues.add(new NameValue("name5", 10));
		nameValues.add(new NameValue("nameA", 3));
		nameValues.add(new NameValue("nameB", 1));
		sortIpCounters(nameValues);
		System.out.println(nameValues);
		
	}
	
}
