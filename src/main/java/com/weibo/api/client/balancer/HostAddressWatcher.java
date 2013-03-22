package com.weibo.api.client.balancer;

import java.util.Set;

/**
 * 
 * Watch the addresses of hostname, notify EndpointManager if the host addresses change.
 * 
 * <p>
 * Now only the endpointManager need knows the change event.
 * If more objects need know, change the watcher to observer-model.
 * </p>
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-8
 */

public interface HostAddressWatcher {

	Set<String> register(String hostname, HostAddressListener listener);
	
	void tryWatchHostAddressImmediately(String hostname);
}
