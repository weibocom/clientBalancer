package com.weibo.api.client.balancer.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool.impl.GenericObjectPool;

import com.weibo.api.client.balancer.Endpoint;
import com.weibo.api.client.balancer.EndpointBalancerException;
import com.weibo.api.client.balancer.EndpointPool;
import com.weibo.api.client.balancer.ResourceClientSwitcher;
import com.weibo.api.client.balancer.util.ClientBalancerLog;
import com.weibo.api.client.balancer.util.ClientBalancerStatLog;
import com.weibo.api.client.balancer.util.ClientBalancerUtil;
import com.weibo.api.client.balancer.util.SystemTimer;

/**
 * 
 * Encapsulate GeneralObjectPool. 
 *
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-8
 */

public class EndpointPoolImpl<R> implements EndpointPool<R>{

	/** 默认为true，当需要人工停止服务时设为false，设置需要外部人工干预 */
	private AtomicBoolean keepService = new AtomicBoolean(true);
	
	/** 默认为true，当server连续失败n次后自动设为false，待自动监测正常后设为true，恢复服务 */
	private AtomicBoolean healthy = new AtomicBoolean(true);
	private AtomicInteger continueFalseCount = new AtomicInteger(0);
	
	/** 连续失败N次后，设置server healthy为false */
	private int continueFalseCountMinThreshold = 10;
	private int continueFalseCountMaxThreshold = 30;
	
	private static String SUFFIX_BORROW_SUCCEED = "_borrow_succeed";
	
	private static String SUFFIX_UNHEALTHY = "_unhealthy";
	private static String SUFFIX_BORROW_FALSE_FOR_STATE = "_borrow_false_for_state";
	private static String SUFFIX_BORROW_FALSE_FOR_OTHER = "_borrow_false_for_other";
	
	private GenericObjectPool<Endpoint<R>> internalPool;
	private EndpointFactory<R> endpointFactory;
	
	/** Dns更新后，把最新的ips设入该set，方便清理下线的ips,清理完毕后，再清空 */
	private CopyOnWriteArraySet<String> reservedIpsForRefresh = new CopyOnWriteArraySet<String>();
	
	private AtomicBoolean closed = new AtomicBoolean(false);
	
	private static AtomicInteger epPoolIdx = new AtomicInteger(0); 
	
	private boolean monitorPerf = true;
	
	public EndpointPoolImpl(EndpointFactory<R> endpointFactory){
		epPoolIdx.incrementAndGet();
		
		EndpointManagerImpl<R> endpointManager = new EndpointManagerImpl<R>();
		endpointManager.init(this, endpointFactory.getConfig());
		
		this.endpointFactory = endpointFactory;
		this.endpointFactory.addListener(endpointManager);
		
		//start check hostAddress changed event
		HostAddressWatcherImpl.getInstance().register(getConfig().getHostname(), endpointManager);
		
		Set<String> ips = HostAddressWatcherImpl.getInstance().register(getConfig().getHostname(), endpointFactory);

		if (ips != null && !ips.isEmpty()
				&& !ClientBalancerUtil.isEqualSet(new HashSet<String>(endpointFactory.getIpAddresses()), ips)) { 
			// TODO 目前这种方式不是最好的，需要一个地方来确保ips的一致性(防止极端情况下dns解析不一致的问题)，需要再改一下 
			endpointFactory.onHostAddressChanged(getConfig().getHostname(), ips);
		}
		
		if(getConfig().minPoolSize > continueFalseCountMinThreshold){
			this.continueFalseCountMinThreshold = getConfig().getMinPoolSize();
		}
		
		if(getConfig().maxPoolSize > continueFalseCountMaxThreshold){
			this.continueFalseCountMaxThreshold = getConfig().getMaxPoolSize();
		}
		
		this.internalPool = new GenericObjectPool<Endpoint<R>>(endpointFactory, endpointFactory.getConfig().getCommonConfig());
		
		ResourceClientSwitcher.registerEndpintPool(this.endpointFactory.getConfig(), this);
		ClientBalancerLog.log.info("Create endpointPool with config {}", endpointFactory.getConfig());
	}
	
	public void init () {
		int minPoolSize = this.endpointFactory.getConfig().minPoolSize;
		
		if (minPoolSize <= 0) {
			return;
		}
		
		for (int i = 0; i < minPoolSize; i ++) {
			try {
				this.internalPool.addObject();
			} catch (Exception e) {
				ClientBalancerLog.log.error("init endpointPool: addObject error {}", endpointFactory.getConfig().getHostname(), e);
			}
		}
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public Endpoint<R> borrowEndpoint() throws EndpointBalancerException{
		Endpoint<R> endpoint = null;
		try {
			endpoint = borrowEndpoint(true);
			if(endpoint != null){
				return endpoint;
			}
		}catch(Exception e){
			throw new EndpointBalancerException("Error: when borrow endpont:"+e.getMessage(), e);
		} finally{
			if(endpoint == null){
				incFalseCount();
			}
		}
		return endpoint;
    }
	

    /**
     *{@inheritDoc}
     */
    @Override
    public void returnEndpoint(Endpoint<R> endpoint) throws EndpointBalancerException{
    	if(endpoint == null){
    		return;
    	}
    	try {
    		if(isAbandoning(endpoint)){
    			invalidateEndpoint(endpoint, false);
    			ClientBalancerLog.log.info("invalidate endpoint for the ip is abandoned, endpoint={}, reservedIpsForRefresh={}", endpoint, reservedIpsForRefresh);
    			
    		}else if(isExpired(endpoint)){
    			invalidateEndpoint(endpoint, false);
    			ClientBalancerLog.log.info("invalidate endpoint for the ip is expired, endpoint={}, reservedIpsForRefresh={}", endpoint, reservedIpsForRefresh);
    			
    		}else {
    			endpoint.refreshLastAccessTime();
    			internalPool.returnObject(endpoint);
    		}
			
		} catch (Exception e) {
			ClientBalancerLog.log.warn("Warn: exception in returnEndpoint", e);
			throw new EndpointBalancerException("Warn: exception in returnEndpoint", e);
		}
    	continueFalseCount.set(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateEndpoint(Endpoint<R> endpoint) throws EndpointBalancerException{
    	invalidateEndpoint(endpoint, true);
    }

    /**
     *
     *{@inheritDoc}
     */
    @Override
    public void addEndpoint() throws EndpointBalancerException{
    	try {
			internalPool.addObject();
		} catch (Exception e) {
			ClientBalancerLog.log.warn("Warn: exception in addEndpoint", e);
			throw new EndpointBalancerException("Warn: exception in addEndpoint", e);
		}
    }

    /**
     * 
     * {@inheritDoc}
     * 
     */
    @Override
    public int getNumIdle(){
		return internalPool.getNumIdle();
    }

    /**
     *
     *{@inheritDoc}
     */
    @Override
    public int getNumActive(){
		return internalPool.getNumActive();
    }

    /**
     * {@inheritDoc}
     * dns更新后，连接到offline-ip的endpoint需要清理，连往offline-ip的endpoint有两类：idle + active, 清理办法:1）active的在return时进行invalidate; 2)idle的用全部取一遍；（目前只支持对fifo有效） 
     */
    @Override
    public void removeOfflineIdleEndpoints(Set<String> reservedIps) {
    	
    	//set abandonedIps
    	reservedIpsForRefresh.addAll(reservedIps);
    	
    	//循环取endpoints，然后return，利用return时的监测来清理
    	int tryCount = 2 * Math.max(endpointFactory.getConfig().getMaxPoolSize(), (getNumActive() + getNumIdle()));
    	for(int i = 0; i < tryCount; i++){
    		Endpoint<R> endpoint = null;
    		try {
				endpoint = borrowEndpoint();
				returnEndpoint(endpoint);
			} catch (Exception e) {
				invalidateEndpoint(endpoint);
			}
    	}
    	
    	ClientBalancerLog.log.info("Refresh endpoints for host:{} updted, latest ips {}", this.endpointFactory.getConfig().getHostname(), reservedIps);
    }
    
    @Override
    public void removeOfflineEndpointsCompleted(){
    	this.reservedIpsForRefresh.clear();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Endpoint<R> tryInvalidateOneIdleEndpoint(String ipAddress) {
    	int tryCount = 2 * getNumIdle();
    	if(tryCount < 1){
    		tryCount = 1;
    	}
    	for(int i = 0; i < tryCount; i++){
    		Endpoint<R> endpoint = null;
    		try {
				endpoint = borrowEndpoint();
				if(endpoint.ipAddress.equals(ipAddress)){
					invalidateEndpoint(endpoint);
					return endpoint;
				}
				returnEndpoint(endpoint);
			} catch (Exception e) {
				invalidateEndpoint(endpoint);
			}
    	}
    	return null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() throws Exception {
    	if(this.closed.get()){
			return;
		}
    	this.closed.compareAndSet(false, true);
    	//|| internalPool.isClosed()
    	if(internalPool == null){
    		return;
    	}
        try {
            internalPool.close();
        } catch (Exception e) {
        	ClientBalancerLog.log.error("Error: when close:" + this, e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void doCheckHealthy(){
    	if(!keepService.get() || healthy.get()){
    		ClientBalancerLog.log.debug("After watch pool healthy completed, epPool:{}", this);

    		return;
    	}
    	
    	Endpoint<R> endpoint = null;
    	try {
			endpoint = borrowEndpoint(false);
			endpointFactory.validateObject(endpoint);
			returnEndpoint(endpoint);
			compareAndsetHealthy(false, true);
		} catch (Exception e) {
			invalidateEndpoint(endpoint);
		}
    	
		ClientBalancerLog.log.info("After watch pool healthy completed, epPool:{}", this);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void pauseService(){
    	this.keepService.compareAndSet(true, false);
    	this.internalPool.clear();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void restartService(){
    	this.keepService.compareAndSet(false, true);
    	Endpoint<R> endpoint = null;
    	try {
			endpoint = borrowEndpoint();
			returnEndpoint(endpoint);
		} catch (Exception e) {
			invalidateEndpoint(endpoint);
		}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAlive() {
    	return keepService.get() && healthy.get();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIdle() {
    	if(getNumActive() < getNumIdle()
    			&& getNumIdle() > this.endpointFactory.getConfig().getMinPoolSize()){
    		return true;
    	}
    	if(getNumIdle() > 2 * this.endpointFactory.getConfig().getMinPoolSize()){
    		return true;
    	}
    	return false;
    }
    
    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public EndpointBalancerConfig getConfig(){
    	return this.endpointFactory.getConfig();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
    	boolean test = false;
    	if(test){
    		throw new NullPointerException("test");
    	}
    	
    	return new StringBuilder(64)
    		.append("endpointPool-").append(epPoolIdx.get()).append("-").append(getConfig().getHostname()).append(":").append(getConfig().getPort())
    		.append(" [active=").append(getNumActive())
    		.append(", idle=").append(getNumIdle())
    		.append(", keepService=").append(keepService)
    		.append(", healthy=").append(healthy.get())
    		.append(", reservedIpsForRefresh=").append(reservedIpsForRefresh)
    		.append("]").toString();
    }
    
    /**
	 * 
	 * @see EndpointPoolImpl#borrowEndpoint()
	 * 
	 * @param checkServerState
	 * @return
	 * @throws EndpointBalancerException
	 */
	private Endpoint<R> borrowEndpoint(boolean checkServerState) throws EndpointBalancerException{
		if(checkServerState && (!healthy.get() || !keepService.get())){
			ClientBalancerStatLog.inc(getConfig().getHostnamePort() + SUFFIX_BORROW_FALSE_FOR_STATE);
			ClientBalancerLog.fire(getPoolName() + " : Server cannot supply service");	
			throw new EndpointBalancerException(String.format("Server cannot supply service, for healthy=%s, keepService=%s", healthy, keepService));
		}
		
		Endpoint<R> ep = null;
		try {
			ep = internalPool.borrowObject();
			return ep;
		} catch (Exception e) {
			ClientBalancerLog.log.warn("Warn: exception when borrowEndpoint", e);
			throw new EndpointBalancerException("Warn: exception when borrowEndpoint", e);
		} finally{
			statBorrowResult(ep);
		}
    }
	
	private void statBorrowResult(Endpoint<R> ep){
		if(ep != null){
			if(monitorPerf){
				ClientBalancerStatLog.inc(getConfig().getHostnamePort() + SUFFIX_BORROW_SUCCEED);
				ClientBalancerStatLog.inc(getConfig().getHostnamePort() + "_" + ep.ipAddress + SUFFIX_BORROW_SUCCEED);
			}
		}else{
			ClientBalancerStatLog.inc(getConfig().getHostnamePort() + SUFFIX_BORROW_FALSE_FOR_OTHER);
		}
	}
	
	/**
	 * check if the ip of endpoint is abandoning now
	 * @param endpoint
	 * @return
	 */
    private boolean isAbandoning(Endpoint<R> endpoint){
    	//一般情况下abandonedIps的size都为0，可以略去contains的计算
    	return reservedIpsForRefresh.size() > 0 && !reservedIpsForRefresh.contains(endpoint.ipAddress);
    }
    
    /**
     * check if the endpoint is expired: is idle or need to retire.
     * @param endpoint
     * @return
     */
    private boolean isExpired(Endpoint<R> endpoint){
    	boolean isIdle = endpointFactory.getConfig().getMaxIdleTime() > 0 
    			&& (endpointFactory.getConfig().getMaxIdleTime() <= (SystemTimer.currentTimeMillis() - endpoint.lastAccessTime.get()));
    	boolean needRetired = endpointFactory.getConfig().getMaxConnectionAge() > 0 
    			&& (endpointFactory.getConfig().getMaxConnectionAge() <= (SystemTimer.currentTimeMillis() - endpoint.createdTime.get()));
    	return isIdle || needRetired;
    }
    
    /**
     * invalidate a endpiont. If updateFalseCount is true, will increment continueFalseCount
     * 
     * @param endpoint
     * @param updateFalseCount
     */
    private void invalidateEndpoint(Endpoint<R> endpoint, boolean updateFalseCount){
    	if(endpoint == null){
    		return;
    	}
    	
    	try {
			if(updateFalseCount){
				incFalseCount();
			}
		} catch (Exception e) {
			ClientBalancerLog.log.warn("Warn: exception in invalidateEndpoint.incFalseCount", e);
		}
    	
		try {
			internalPool.invalidateObject(endpoint);
		} catch (Exception e) {
			ClientBalancerLog.log.warn("Warn: exception in invalidateEndpoint", e);
			throw new EndpointBalancerException("Warn: exception in invalidateEndpoint", e);
		}
		ClientBalancerLog.log.info("Invalidate a endpoint={} completed! updateFalseCount={}", endpoint, updateFalseCount);
    }
    
    private void incFalseCount(){
    	int falseCount = continueFalseCount.incrementAndGet();
		if(falseCount >= this.continueFalseCountMaxThreshold){
			compareAndsetHealthy(true, false);
			continueFalseCount.set(0);
			
    	}else if(falseCount > this.continueFalseCountMinThreshold){
			HostAddressWatcherImpl.getInstance().tryWatchHostAddressImmediately(getConfig().getHostname());
			
		} 
    }
    
    /**
     * Atomically sets the value of healthy to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     * @return true if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    private boolean compareAndsetHealthy(boolean expect, boolean update){
    	if(!update){
    		ClientBalancerStatLog.inc(getConfig().getHostnamePort() + SUFFIX_UNHEALTHY);
    	}
    	return healthy.compareAndSet(expect, update);
    }
    
    public String getPoolName() {
    	return "Conn<REDIS " + getConfig().getHostnamePort();
    }
}
