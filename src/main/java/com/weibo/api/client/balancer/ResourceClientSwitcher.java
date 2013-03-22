package com.weibo.api.client.balancer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;

import com.weibo.api.client.balancer.impl.EndpointBalancerConfig;
import com.weibo.api.client.balancer.util.ClientBalancerLog;

/**
 * 
 * client switcher for turn on and turn up the client.
 *
 * @author fishermen
 * @version V1.0 created at: 2012-9-12
 */

public class ResourceClientSwitcher {

	private static Map<EndpointBalancerConfig, EndpointPool> endpointPools = new ConcurrentHashMap<EndpointBalancerConfig, EndpointPool>(100);
	
	/**
	 * register a endpointPool
	 * @param config
	 * @param endpointPool
	 */
	public static void registerEndpintPool(EndpointBalancerConfig config, EndpointPool endpointPool){
		if(config == null || endpointPool == null){
			throw new NullPointerException(String.format("Config or epPool is null, config=%s, endpointPool=%s", config, endpointPool));
		}
		
		if(!endpointPools.containsKey(config)){
			endpointPools.put(config, endpointPool);
		}else{
			ClientBalancerLog.log.warn("ClientSwither - ignore duplicate registerEndpintPool, key={}, epPool={}", config, endpointPool);
			return;
		}
		ClientBalancerLog.log.warn("ClientSwither - registerEndpintPool, key={}, epPool={}", config, endpointPool);
	}
	
	/**
	 * Pauses service of the endpointPool with the given hostname and port
	 * @param hostname
	 * @param port
	 * @return
	 */
	public static boolean pauseService(String hostname, int port){
		ClientBalancerLog.log.warn("ClientSwither - pauseService start, hostname={}, port={}", hostname, port);
		
		int pauseCount = 0;
		for(EndpointBalancerConfig config : endpointPools.keySet()){
			if(StringUtils.equals(config.getHostname(), hostname)
					&& config.getPort() == port){
				endpointPools.get(config).pauseService();
				ClientBalancerLog.log.info("ClientSwither - pauseService success! hostname={}, port={}", hostname, port);
				pauseCount++;
			}
		}
		
		ClientBalancerLog.log.warn("ClientSwither - pause endpointPool count={}, hostname:port={}:{}", new Object[]{pauseCount, hostname, port});
		return true;
	}
	
	/**
	 * restart service of the endpointPool with the given hostname and port
	 * @param hostname
	 * @param port
	 * @return
	 */
	public static boolean restartService(String hostname, int port){
		ClientBalancerLog.log.warn("ClientSwither - restartService start, hostname={}, port={}", hostname, port);
		int restartCount = 0;
		for(EndpointBalancerConfig config : endpointPools.keySet()){
			if(StringUtils.equals(config.getHostname(), hostname)
					&& config.getPort() == port){
				endpointPools.get(config).restartService();
				ClientBalancerLog.log.warn("ClientSwither - restart endpointPool success! hostname={}, port={}", hostname, port);
				restartCount++;
			}
		}
		
		ClientBalancerLog.log.warn("ClientSwither - restart endpointPool count={}, hostname:port={}:{}", new Object[]{restartCount, hostname, port});
		return true;
	}
	
}
