package com.shekhar.cxs.configuration.service.api;

import java.util.Map;

/**
 * @author Sangala Shekhar Reddy
 */
public interface ConfigurationService {

    /**
     * Returns a Map<String, Node> with the tree heaving the parentPath as root
     *
     * @param parentFolder
     * @return
     */
    public Map<String, Node> getAllNodes(String parentFolder);

    /**
     * setter for etcd property (key)
     *
     * @param path
     * @param value
     * @throws Exception
     */
    public void set(String path, byte[] value) throws Exception;
    
    /**
     * removes the "path" from etcd
     * 
     * @param key
     * @throws Exception
     */
    public void remove(String key) throws Exception;
    
    /**
     * The getter for etcd property
     * 
     * @param key
     * @return
     */
    public byte[] get(String key);
    
    /**
     * Adds a NodeListener to a specific path from etcd
     * 
     * @param path
     * @param listener
     */
    public void addListenerToPath(String path, NodeListener listener);

    /**
     * Unregisters all path listeners
     */
    public void unregisterAllListeners();

    /**
     * creates the "recordMap" parameters in etcd in a single transaction.
     * 
     * @param recordMap
     * @throws Exception
     */
    public void atomicBatchUpdate(Map<String, byte[]> recordMap) throws Exception;

    /**
     * deletes the "deletePath" from etcd and creates the "recordMap" parameters in etcd in a single transaction.
     * 
     * @param recordMap
     * @param deletePath
     * @throws Exception
     */
    public void atomicBatchUpdate(Map<String, byte[]> recordMap, String deletePath) throws Exception;

}
