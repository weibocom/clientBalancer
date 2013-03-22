package com.weibo.api.client.balancer.impl;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;

/**
 * 
 * Configurations
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-9
 */

public class EndpointBalancerConfig {
	
	/** server hostname & port */
	protected String hostname;
	protected int port;
	
	/** min/max number of clients a pool will maintain at any given time  */
	protected int minPoolSize = 2;
	protected int maxPoolSize = 15;
	
	/** Milliseconds, pool watch period */
	protected int poolWatchInterval = 60 * 1000;
	
	/** Milliseconds, pool healthy watch period */
	protected int poolHealthyInterval = 1 * 1000;
	
	/** Milliseconds a client can remain pooled, zero means idle client never expire. */
	protected int maxIdleTime = 3 * 60 * 1000;
	
	/** Milliseconds, effective a time to live */
	protected int maxConnectionAge = 10 * 60 * 60 * 1000;
	
	/** Milliseconds, tcp socket timeout */
	protected int soTimeout = 1000;
	
	/** Milliseconds, client operation(like query/insert/del) timeout */
	protected int opTimeout = 1000;

	/** Config 里的大部分参数可以在此类中通过setters设置, 除了maxIdle、maxActive、minIdle，这三个参数通过min/maxPoolSize来设置*/
	public Config commonConfig = null;
	
	public EndpointBalancerConfig(){
		commonConfig = new Config();
		
		//init Config
		initCommonConfig();
	}
	
	private void initCommonConfig(){
		// test 1/4 objs per run
		commonConfig.numTestsPerEvictionRun = -5;
		commonConfig.timeBetweenEvictionRunsMillis = 2 * 60 * 1000;
		commonConfig.testWhileIdle = true;
		// 1 hour
		commonConfig.softMinEvictableIdleTimeMillis = 3600 * 1000;
		commonConfig.minEvictableIdleTimeMillis = -1;
	

		commonConfig.maxWait = 500;
		commonConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;

		commonConfig.lifo = false;
	}
	
	public int getPoolWatchInterval() {
		return poolWatchInterval;
	}

	public int getPoolHealthyInterval() {
		return poolHealthyInterval;
	}

	public void setPoolHealthyInterval(int poolHealthyInterval) {
		this.poolHealthyInterval = poolHealthyInterval;
	}

	public void setPoolWatchInterval(int poolWatchInterval) {
		if(poolWatchInterval > 0){
			this.poolWatchInterval = poolWatchInterval;
		}
	}

	public int getMinPoolSize() {
		return minPoolSize;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public Config getCommonConfig() {
		return commonConfig;
	}

	public void setCommonConfig(Config commonConfig) {
		this.commonConfig = commonConfig;
	}

	public String getHostname() {
		return hostname;
	}
	
	public String getHostnamePort(){
		return getHostname() + ":" + getPort(); 
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public int getSoTimeout() {
		return soTimeout;
	}

	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}
	

	public int getOpTimeout() {
		return opTimeout;
	}

	public void setOpTimeout(int opTimeout) {
		this.opTimeout = opTimeout;
	}

	public int getMaxIdleTime() {
		return maxIdleTime;
	}

	public void setMaxIdleTime(int maxIdleTime) {
		this.maxIdleTime = maxIdleTime;
	}

	public int getMaxConnectionAge() {
		return maxConnectionAge;
	}

	public void setMaxConnectionAge(int maxConnectionAge) {
		this.maxConnectionAge = maxConnectionAge;
	}

	/***************************
	 * 
	 * commons.pool.Config setters & getters
	 * 
	 ***************************/
	public void setMaxWait(long maxWait) {
		this.commonConfig.maxWait = maxWait;
	}

	public void setWhenExhaustedAction(byte whenExhaustedAction) {
		this.commonConfig.whenExhaustedAction = whenExhaustedAction;
	}

	public void setTestOnBorrow(boolean testOnBorrow) {
		this.commonConfig.testOnBorrow = testOnBorrow;
	}

	public void setTestOnReturn(boolean testOnReturn) {
		this.commonConfig.testOnReturn = testOnReturn;
	}

	public void setTestWhileIdle(boolean testWhileIdle) {
		this.commonConfig.testWhileIdle = testWhileIdle;
	}

	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		this.commonConfig.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
		this.commonConfig.numTestsPerEvictionRun = numTestsPerEvictionRun;
	}

	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		this.commonConfig.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	public void setSoftMinEvictableIdleTimeMillis(long softMinEvictableIdleTimeMillis) {
		this.commonConfig.softMinEvictableIdleTimeMillis = softMinEvictableIdleTimeMillis;
	}

	public void setLifo(boolean lifo) {
		this.commonConfig.lifo = lifo;
	}

	public void setMinPoolSize(int minPoolSize) {
		this.minPoolSize = minPoolSize;
		this.commonConfig.minIdle = minPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.commonConfig.maxIdle = maxPoolSize;
		this.maxPoolSize = maxPoolSize;
		this.commonConfig.maxActive = maxPoolSize;
	}
	
	@Override
	public String toString() {
		return new StringBuilder(512)
			.append("ClientBalancerConfig - { host=").append(hostname)
			.append(", port=").append(port)
			.append(", minPoolSize=").append(minPoolSize)
			.append(", maxPoolSize=").append(maxPoolSize)
			.append(", poolWatchInterval=").append(poolWatchInterval)
			.append(", maxIdleTime=").append(maxIdleTime)
			.append(", maxConnectionAge=").append(maxConnectionAge)
			.append(", soTimeout=").append(soTimeout)
			.append(", opTimeout=").append(opTimeout)
			.append(", CommonConfig: [")
					.append("ccof.maxIdle=").append(this.commonConfig.maxIdle)
					.append(", ccof.minIdle=").append(this.commonConfig.minIdle)
					.append(", ccof.maxActive=").append(this.commonConfig.maxActive)
					.append(", ccof.maxWait=").append(this.commonConfig.maxWait)
					.append(", ccof.whenExhaustedAction=").append(this.commonConfig.whenExhaustedAction)
					.append(", ccof.testOnBorrow=").append(this.commonConfig.testOnBorrow)
					.append(", ccof.testOnReturn=").append(this.commonConfig.testOnReturn)
					.append(", ccof.testWhileIdle=").append(this.commonConfig.testWhileIdle)
					.append(", ccof.timeBetweenEvictionRunsMillis=").append(this.commonConfig.timeBetweenEvictionRunsMillis)
					.append(", ccof.numTestsPerEvictionRun=").append(this.commonConfig.numTestsPerEvictionRun)
					.append(", ccof.minEvictableIdleTimeMillis=").append(this.commonConfig.minEvictableIdleTimeMillis)
					.append(", ccof.softMinEvictableIdleTimeMillis=").append(this.commonConfig.softMinEvictableIdleTimeMillis)
					.append(", ccof.lifo=").append(this.commonConfig.lifo)
					.append("]")
			.append("}").toString();
	}
	
}
