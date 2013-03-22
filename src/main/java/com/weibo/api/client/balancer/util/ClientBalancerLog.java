package com.weibo.api.client.balancer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 
 * ClientBalancer logger
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-17
 */

public class ClientBalancerLog {

	public static long REDIS_FIRE_TIME=300; //Redis操作超时
	
	public static Logger log = LoggerFactory.getLogger("com.weibo.api.client.balancer");
	public static Logger fireLog = LoggerFactory.getLogger("fire");
	//public static Logger statLog = LoggerFactory.getLogger("statlog");
	
	public static void fire(String msg) {
		if (fireLog.isInfoEnabled()) {
			fireLog.info(msg);
		}
	}
}
