/*
 *  Copyright (c) 2015 by Alcatel-Lucent and shekhar, Inc. All rights reserved.
 *
 *  CONFIDENTIAL. All rights reserved. This computer program is
 *  protected under Copyright. Recipient is to retain the program in
 *  confidence, and is not permitted to copy, use, distribute, modify or
 *  translate the program without authorization
 *
 *  @author  Sangala Shekhar Reddy
 *
 *  $Header$
 */
package com.shekhar.mas.configuration.service.api.impl;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


import com.shekhar.mas.configuration.service.api.Node;
import com.shekhar.mas.configuration.service.api.NodeListener;
import com.shekhar.mas.configuration.service.api.TimeDelayedNodeListener;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;

import org.apache.curator.framework.api.transaction.CuratorTransaction;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException;

import com.shekhar.mas.configuration.service.api.ConfigurationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author ccayirog
 */
public class ConfigurationServiceImpl implements ConfigurationService {

    //this can have the following format server1:port1,server2:port2
    @Value("${zookeeper.connectionString}")
    private String zkConnectionString;


    @Value("${zookeeper.namespace}")
    private String zkNamespace;

    private String zkUser;
    private String zkPass;
    private static final int WAIT_BEFORE_CLOSE = 11000;

    private static final int RETRY_COUNT = 3;
    private static final int WAIT_BEFORE_RETRY = 1000;
    private static final String CACHE_ROOT = "/";
    private static final String PATH_SEPARATOR = "/";
    private static final Pattern NAME_SPACE_PATTERN = Pattern.compile("^[\\w]+(\\.\\w+)*$");
    private static final long NOTIFICATION_DELAY = 30000;

    private List<TimeDelayedNodeListener> timeDelayedNodeListeners;

    private TreeCache cache;

    private boolean initialized = false;

    private CuratorFramework client;

    private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

	private TreeCache tryInitCache(CuratorFramework curatorClient) throws Exception{
		TreeCache theCache = new TreeCache(curatorClient, CACHE_ROOT);
	    CountDownLatch countDownLatch = new CountDownLatch(1);
	    theCache.getListenable().addListener((client1, event) -> {
	        if (event.getType().equals(TreeCacheEvent.Type.INITIALIZED)) {
	            countDownLatch.countDown();
	        }
	    });
	    theCache.start();
	    if (!countDownLatch.await(30, TimeUnit.SECONDS)) {
	        log.error("Configuration service cache could not initialize");
	        theCache.close();
	        theCache=null;
	    }
	    return theCache;
	}
	
	private boolean initCache(CuratorFramework curatorClient) throws Exception{
		 int retry=0;
		 this.cache = null;
	     while (null == this.cache && retry< RETRY_COUNT){
	    	log.info("initCache:" + Integer.toString(retry+1));
	     	this.cache = tryInitCache(curatorClient);
	     	retry++;
	     }
	     if(null == this.cache){
	    	 return false;
	     }
	     return true;
	}

    /**
     * The init() function is called automatically by Spring in @PostConstruct phase.
     * You need to call this function manually if you are not using spring.
     *
     * @throws Exception
     */

    @PostConstruct
    public void init() throws Exception {
        if (initialized) {
            log.info("Configuration service was already initialized");
            return;
        }
        initialized = true;
        log.info("Starting ConfigurationServiceImpl");
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(WAIT_BEFORE_RETRY, RETRY_COUNT);
        String namespace = convert2Path(zkNamespace);
        this.client = CuratorFrameworkFactory.
                builder().
                connectString(zkConnectionString).
                retryPolicy(retryPolicy).
                namespace(namespace).
                build();
        this.client.start();
        this.client.blockUntilConnected();
        log.info("client:"+this.client.toString());
        if(!initCache(this.client)){
        	log.error("ConfigurationServiceImpl could not initialize cache!"+" - client state:"+this.client.getState()+" client namespace:"+this.client.getNamespace()+" client zookeeper:"+this.client.getZookeeperClient().getCurrentConnectionString()+" client data:"+this.client.getData()+" client:"+this.client);
        };
        
        this.timeDelayedNodeListeners = new ArrayList<>();
        log.info("ConfigurationServiceImpl started.");
    }

    /**
     * Returns a Map<String, Node> with the tree heaving the parentPath as root
     *
     * @param parentFolder
     * @return
     */
    @Override
    public Map<String, Node> getAllNodes(String parentPath) {
        Map<String, Node> nodeMap = new HashMap<>();
        getAllNodes(parentPath, nodeMap);
        return nodeMap;
    }

    /**
     * Returns by reference the updated Map<String, Node> nodeMap with the tree heaving the parentPath as root
     *
     * @param parentPath
     * @param nodeMap
     */
    public void getAllNodes(String parentPath, Map<String, Node> nodeMap) {
        if (parentPath.length() > 1) {
            parentPath = parentPath.replaceAll("/$", "");
        }
        Map<String, ChildData> currentChildren = cache.getCurrentChildren(parentPath);
        if (currentChildren != null) {
            currentChildren.values().stream().forEach(node -> {
                nodeMap.put(node.getPath(), new Node(node.getPath(), node.getData()));
                if (node.getStat() != null && node.getStat().getNumChildren() > 0) {
                    getAllNodes(node.getPath(), nodeMap);
                }
            });
        }
    }

    /**
     * setter for zookeeper property (key)
     *
     * @param key
     * @param value
     * @throws Exception
     */
    @Override
    public void set(String key, byte[] value) throws Exception {
        try {
            this.client.setData().forPath(key, value);
        } catch (KeeperException.NoNodeException e) {
            this.client.create().creatingParentsIfNeeded().forPath(key, value);
        }
    }

    /**
     * removes the "path" from zookeeper
     *
     * @param path
     * @throws Exception
     */
    @Override
    public void remove(String path) throws Exception {
        this.client.delete().guaranteed().forPath(path);

    }

    /**
     * deletes the "deletePath" from zookeeper and creates the "recordMap" parameters in zookeeper in a single transaction.
     *
     * @param recordMap
     * @param deletePath
     * @throws Exception
     */
    @Override
    public void atomicBatchUpdate(Map<String, byte[]> recordMap, String deletePath) throws Exception {
        CuratorTransaction curatorTransaction = this.client.inTransaction();
        CuratorTransactionFinal transactionFinal = null;

        Set<String> set = new HashSet<>();
        for (String path : recordMap.keySet()) {
            String[] split = path.split(PATH_SEPARATOR);
            String currentPath = "";
            for (String subPath : split) {
                if (!subPath.isEmpty()) {
                    currentPath += PATH_SEPARATOR + subPath;
                    set.add(currentPath);
                }
            }
        }

        List<String> nonExist = new ArrayList<>();
        for (String path : set) {
            if (client.checkExists().forPath(path) == null) {
                nonExist.add(path);
            }
        }

        Set<String> paths = new HashSet<>();
        Collections.sort(nonExist);
        for (String path : nonExist) {
            if (transactionFinal != null) {
                transactionFinal = transactionFinal.create().forPath(path, new byte[0]).and();
                paths.add(path);
            } else {
                transactionFinal = curatorTransaction.create().forPath(path, new byte[0]).and();
                paths.add(path);
            }
        }
        
        transactionFinal = resetNodesValues(curatorTransaction, transactionFinal, set);
        
        for (Map.Entry<String, byte[]> e : recordMap.entrySet()) {
        	System.out.println("e.getKey(): "+e.getKey());
            if (transactionFinal != null) {
                transactionFinal = transactionFinal.setData().forPath(e.getKey(), e.getValue()).and();
                paths.add(e.getKey());
            } else {
                transactionFinal = curatorTransaction.setData().forPath(e.getKey(), e.getValue()).and();
                paths.add(e.getKey());
            }
        }
        
        if (deletePath != null) {
            Map<String, Node> deleteNodes = getAllNodes(deletePath);
            List<String> list = new ArrayList<>(deleteNodes.size());
            list.addAll(deleteNodes.keySet());
            Collections.sort(list);
            Collections.reverse(list);
            for (String path : list) {
                if (paths.stream().filter(p -> p.startsWith(path)).count() == 0) {
                	System.out.println("deleting: "+path);
                    if (transactionFinal != null) {
                        transactionFinal = transactionFinal.delete().forPath(path).and();
                    } else {
                        transactionFinal = curatorTransaction.delete().forPath(path).and();
                    }
                }
            }
        }

        if (transactionFinal != null) {
            transactionFinal.commit();
        }


    }

    /**
     * Set the value (new byte[0]) to all paths from Set<String>
     * 
     * @param curatorTransaction
     * @param transactionFinal
     * @param paths
     * @return
     * @throws Exception
     */
	private CuratorTransactionFinal resetNodesValues(CuratorTransaction curatorTransaction,
														CuratorTransactionFinal transactionFinal, 
														Set<String> paths) throws Exception {
		for (String node:paths){
			System.out.println("node "+node);
        	if (transactionFinal != null) {
                transactionFinal = transactionFinal.setData().forPath(node, new byte[0]).and();
            } else {
                transactionFinal = curatorTransaction.setData().forPath(node, new byte[0]).and();
            }
        }
		return transactionFinal;
	}
    
    /**
     * creates the "recordMap" parameters in zookeeper in a single transaction.
     *
     * @param recordMap
     */
    @Override
    public void atomicBatchUpdate(Map<String, byte[]> recordMap) throws Exception {
        atomicBatchUpdate(recordMap, null);
    }

    /**
     * The getter for zookeeper property
     *
     * @param key
     */
    @Override
    public byte[] get(String key) {
        ChildData currentData = this.cache.getCurrentData(key);
        return currentData != null ? currentData.getData() : null;
    }

    /**
     * Registers a TreeCacheListener
     *
     * @param treeCacheListener
     */
    public void addListenerToTreeCache(TreeCacheListener treeCacheListener) {
        this.cache.getListenable().addListener(treeCacheListener);
    }

    /**
     * Converts appName to path by replacing \ with PATH_SEPARATOR.
     *
     * @param appName
     * @return the new appName
     */
    private String convert2Path(String appName) {
        validateAppName(appName);
        return appName.replaceAll("\\.", PATH_SEPARATOR);
    }

    /**
     *
     * @param appName
     */
    private void validateAppName(String appName) {
        if (appName == null)
            throw new IllegalArgumentException("Null namespace name is not allowed.");

        if (!appName.isEmpty() && !NAME_SPACE_PATTERN.matcher(appName).matches()) {
            throw new IllegalArgumentException("Invalid namespace name: " + appName);
        }

    }

    /**
     * Adds a NodeListener to a specific path from zookeeper
     *
     * @param path
     * @param listener
     */
    @Override
    public void addListenerToPath(final String path, NodeListener listener) {
        addListenerToPath(path, listener, NOTIFICATION_DELAY);
    }

    /**
     * Adds a NodeListener for a specific path from zookeeper which may work asynchronous.
     *
     * @param path
     * @param listener
     * @param notificationDelay
     */
    public void addListenerToPath(final String path, NodeListener listener, long notificationDelay) {
        final TimeDelayedNodeListener timeDelayedNodeListener = new TimeDelayedNodeListener(path, listener, notificationDelay);
        this.cache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
                if (event.getData() != null && check(event)) {
                    timeDelayedNodeListener.insertEvent(event);
                }
            }

            private boolean check(TreeCacheEvent event) {
                return event.getData().getPath().startsWith(path);
            }
        });
        timeDelayedNodeListeners.add(timeDelayedNodeListener);
        timeDelayedNodeListener.start();
    }

    /**
     * Called by spring in @PreDestroy phase. You shall call this method yourself if you created the object yourself (not using Spring)
     */
    @PreDestroy
    public void close() {
        log.debug("Closing ConfigurationServiceImpl...cache...");
        this.cache.close();
        log.debug("Closing ConfigurationServiceImpl...client...");
        try {
            this.client.close();
            log.debug("Closing ConfigurationServiceImpl...connected clients...");
            this.timeDelayedNodeListeners.forEach(TimeDelayedNodeListener::stop);
            log.debug("Will sleep for " + Integer.toString(WAIT_BEFORE_CLOSE) + " milisec...");
            Thread.sleep(WAIT_BEFORE_CLOSE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * setter for zkConnectionString. This parameter can have the following format server1:port1,server2:port2
     *
     * @param conn
     */
    public void setZkConnectionString(String conn) {
        this.zkConnectionString = conn;
    }

    /**
     * getter for zkConnectionString
     *
     * @return
     */
    public String getZkConnectionString() {
        return this.zkConnectionString;
    }

    /**
     * getter for zkNamespace
     *
     * @return
     */
    public String getZkNamespace() {
        return zkNamespace;
    }

    /**
     * setter for zkNamespace
     *
     * @param zkNamespace
     */
    public void setZkNamespace(String zkNamespace) {
        this.zkNamespace = zkNamespace;
    }

    /**
     * getter for zkUser
     *
     * @return
     * @deprecated
     */
    public String getZkUser() {
        return zkUser;
    }

    /**
     * setter for zkUser
     *
     * @param zkUser
     * @deprecated
     */
    public void setZkUser(String zkUser) {
        this.zkUser = zkUser;
    }

    /**
     * setter for zkPass
     *
     * @return
     * @deprecated
     */
    public String getZkPass() {
        return zkPass;
    }

    /**
     * setter for zkPass
     *
     * @param zkPass
     * @deprecated
     */
    public void setZkPass(String zkPass) {
        this.zkPass = zkPass;
    }


    @Override
    public void unregisterAllListeners() {
        this.timeDelayedNodeListeners.forEach(TimeDelayedNodeListener::stop);
    }
}
