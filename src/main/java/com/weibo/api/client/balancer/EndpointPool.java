package com.weibo.api.client.balancer;

import java.util.Set;

import org.apache.commons.pool.PoolableObjectFactory;

import com.weibo.api.client.balancer.impl.EndpointBalancerConfig;


/**
 * 
 * For encapsulate GeneralObjectPool. 
 *
 * @author fishermen
 * @version V1.0 created at: 2012-8-8
 */

public interface EndpointPool<R> {

	/**
     * Obtains an instance from this pool.
     *
     * @return an instance from this pool.
     * @throws EndpointBalancerException
     */
	Endpoint<R> borrowEndpoint() throws EndpointBalancerException;

    /**
     * Return an instance to the pool.
     * By contract, <code>endpoint</code> <strong>must</strong> have been obtained
     * using {@link #borrowObject() borrowObject}
     * or a related method as defined in an implementation
     * or sub-interface.
     *
     * @param endpoint a {@link #borrowObject borrowed} instance to be returned.
     * @throws EndpointBalancerException 
     */
    void returnEndpoint(Endpoint<R> endpoint) throws EndpointBalancerException;

    /**
     * <p>Invalidates an object from the pool.</p>
     * 
     * <p>By contract, <code>obj</code> <strong>must</strong> have been obtained
     * using {@link #borrowObject borrowObject} or a related method as defined in
     * an implementation or sub-interface.</p>
     *
     * <p>This method should be used when an object that has been borrowed
     * is determined (due to an exception or other problem) to be invalid.</p>
     *
     * @param endpoint a {@link #borrowObject borrowed} instance to be disposed.
     * @throws EndpointBalancerException
     */
    void invalidateEndpoint(Endpoint<R> endpoint) throws EndpointBalancerException;

    /**
     * try to invalidate one endpoint by ipAddress, invalidate nothing if no endpoint with the ipAddress is idle.
     * @param ipAddress
     */
    Endpoint<R> tryInvalidateOneIdleEndpoint(String ipAddress);
    
    /**
     * remove offline idle endpoints, only leave the endpoints with the ip in the reservedIps
     * 
     * @param abandonedIpAddresses
     */
    void removeOfflineIdleEndpoints(Set<String> reservedIps);
    
    /**
     * notify the pool: remove offline endpoints completed
     */
    void removeOfflineEndpointsCompleted();
    
    /**
     * Create an object using the {@link PoolableObjectFactory factory} or other
     * implementation dependent mechanism, passivate it, and then place it in the idle object pool.
     * <code>addObject</code> is useful for "pre-loading" a pool with idle objects.
     * (Optional operation).
     *
     * @throws EndpointBalancerException
     */
    void addEndpoint() throws EndpointBalancerException;

    /**
     * Return the number of instances
     * currently idle in this pool (optional operation).
     * This may be considered an approximation of the number
     * of objects that can be {@link #borrowObject borrowed}
     * without creating any new instances.
     * Returns a negative value if this information is not available.
     *
     * @return the number of instances currently idle in this pool or a negative value if unsupported
     * @throws EndpointBalancerException
     */
    int getNumIdle();

    /**
     * Return the number of instances
     * currently borrowed from this pool
     * (optional operation).
     * Returns a negative value if this information is not available.
     *
     * @return the number of instances currently borrowed from this pool or a negative value if unsupported
     * @throws EndpointBalancerException
     */
    int getNumActive();
    
    /**
     * Close this pool, and free any resources associated with it.
     * <p>
     * Calling {@link #addObject} or {@link #borrowObject} after invoking
     * this method on a pool will cause them to throw an
     * {@link IllegalStateException}.
     * </p>
     *
     * @throws Exception <strong>deprecated</strong>: implementations should silently fail if not all resources can be freed.
     */
    void close() throws Exception;
    
    /**
     * Return the state of the pool
     * @return
     */
    boolean isAlive();
    
    /**
     * Return if the pool is idle (not busy!)
     * @return
     */
    boolean isIdle();
    
    /**
     * Start service
     */
    void restartService();
    
    /**
     * Pause service, and the borrowEndpoint() will return null 
     */
    void pauseService();
    
    /**
     * check if the server is healthy
     */
    void doCheckHealthy();
    
    /**
     * return the config for this endpointPool
     * @return
     */
    EndpointBalancerConfig getConfig();
}
