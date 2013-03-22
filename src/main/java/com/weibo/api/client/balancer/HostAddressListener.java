package com.weibo.api.client.balancer;

import java.util.Set;

/**
 * 
 * listening the hostAddress change event
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-9
 */

public interface HostAddressListener {

	/**
	 * notify the listener that the hostAddress changed
	 * @param hostname
	 * @param latestIps
	 */
	void onHostAddressChanged(String hostname, Set<String> latestIps);
}
