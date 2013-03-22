package com.weibo.api.client.balancer.util;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * Provide utility methods that can be used by clientBalancer to perform common operations;
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-8
 */

public class ClientBalancerUtil {
	
	public static final String CHARSET_UTF8 = "UTF-8";
	
	/**
	 * Gets all the ip address from the name service;
	 * @param hostname
	 * @return ips or throws IllegalArgumentException if the hostname is unknown
	 */
	public static Set<String> getAllIps(String hostname){
		//name service 可能会不稳定而解析失败，增加重试
		Set<String> ips = new HashSet<String>();
		int tryCount = 0;
		while(tryCount++ < 3){
			try {
				InetAddress[] addresses = InetAddress.getAllByName(hostname);
				for(InetAddress ia : addresses){
					ips.add(ia.getHostAddress());
				}
				if(ips.size() > 0){
					ClientBalancerLog.log.info("Hostname {}, ips={}", hostname, ips);
					return ips;
				}
			} catch (UnknownHostException e) {
				ClientBalancerLog.log.error("The hostname {} is unknown! tryCount={}", hostname, tryCount);
				safeSleep(nextRandom(100));
			}
		}
		ClientBalancerLog.log.warn("Not found ips, hostname {}, ips={}", hostname, ips);
		return ips;
	}
	
	
	public static void safeSleep(int millis){
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			ClientBalancerLog.log.error("Error when thread sleep",  e);
		}
	}
	
	public static int next(int n){
		return (int)(Math.random() * n);
	}
	
	 public static String toStr(byte[] data) {
	        if (data == null) {
	            return null;
	        }

	        try {
	            return new String(data, CHARSET_UTF8);
	        } catch (UnsupportedEncodingException e) {
	            throw new RuntimeException("Error byte[] to String => " + e);
	        }
	    }
	    public static byte[] toBytes(String str) {
	        try {
	            return str.getBytes(CHARSET_UTF8);
	        } catch (Exception e) {
	            throw new RuntimeException("Error serializing String:" + str + " => " + e);
	        }
	    }
	

	/**
	 * check if the two sets are equal
	 * 
	 * @param set1
	 * @param set2
	 * @return
	 */
	public static boolean isEqualSet(Set<String> set1, Set<String> set2) {
		if (set1 == null && set2 == null) {
			return true;
		} else if (set1 == null || set2 == null) {
			return false;
		}

		if (set1.size() != set2.size()) {
			return false;
		}

		return set2.containsAll(set1);

	}
	    
	private static int nextRandom(int seed){
		return (int)(Math.random() * seed);
	}
}
