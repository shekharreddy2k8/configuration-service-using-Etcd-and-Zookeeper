package com.shekhar.cxs.configuration.service.api.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import mousio.client.promises.ResponsePromise;
import mousio.client.retry.RetryPolicy;
import mousio.client.retry.RetryWithExponentialBackOff;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;

import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.shekhar.cxs.configuration.service.api.ConfigurationService;
import com.shekhar.cxs.configuration.service.api.Node;
import com.shekhar.cxs.configuration.service.api.NodeListener;
import com.shekhar.cxs.configuration.service.api.TimeDelayedEtcdNodeListener;

/**
 * @author Sangala Shekhar Reddy
 */
public class ConfigurationServiceEtcdImpl implements ConfigurationService {

	
    @Value("${etcd.namespace}")
    private String etcdNameSpace;

    private EtcdClient etcd = null;
    
    @Value("${etcd.connectionString}")
    private String etcdConnectionString;

    private static final int WAIT_BEFORE_CLOSE = 11000;

    private static final int RETRY_COUNT = 3;
    private static final int WAIT_BEFORE_RETRY = 1000;
    
    
    private static final String PATH_SEPARATOR = "/";
    private static final Pattern NAME_SPACE_PATTERN = Pattern.compile("^[\\w]+(\\.\\w+)*$");
    private static final long NOTIFICATION_DELAY = 30000;

    private List<TimeDelayedEtcdNodeListener> timeDelayedEtcdNodeListeners;

    private boolean initialized = false;

    private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceEtcdImpl.class);

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
         log.info("Starting ConfigurationServiceEtcdImpl");
         RetryPolicy retryPolicy = new RetryWithExponentialBackOff(WAIT_BEFORE_RETRY, RETRY_COUNT,WAIT_BEFORE_RETRY);
         this.etcd=new EtcdClient(new URI(etcdConnectionString));
         this.etcd.setRetryHandler(retryPolicy);
         this.timeDelayedEtcdNodeListeners = new ArrayList<>();
         log.info("ConfigurationServiceEtcdImpl started.");
       
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
        try {
			getAllNodes(null,PATH_SEPARATOR+etcdNameSpace+parentPath, nodeMap);
		} catch (Exception e) {
			log.error("ERROR in getAllNodes:"+e.getMessage());
		}
        return nodeMap;
    }
    

    /**
     * Returns by reference the updated Map<String, Node> nodeMap with the tree heaving the parentPath as root
     *
     * @param parentPath
     * @param nodeMap
     */
    public void getAllNodes(EtcdNode etcdNode, String directoryPath,Map<String, Node> nodeMap) throws Exception {
    	
    	EtcdResponsePromise<EtcdKeysResponse> promise = null;
        if (etcdNode == null) {
               promise = etcd.getDir(directoryPath).recursive().send();
               EtcdKeysResponse etcdKeysResponse= promise.get();
               List<EtcdNode> nodes=etcdKeysResponse.node.nodes;
            for (EtcdNode etcdNodeChild : nodes) {
                getAllNodes(etcdNodeChild,null,nodeMap);
               }
        } else {
               if (etcdNode.dir) {
                     promise = etcd.getDir(etcdNode.key).recursive().send();
                     EtcdKeysResponse keyR= promise.get();
                     List<EtcdNode> nodes1=keyR.node.nodes;
                     for(EtcdNode etcdNode2 : nodes1) {
                            if (etcdNode2.dir) {
                                   getAllNodes(etcdNode2,null,nodeMap);
                            } else {
                                   if(etcdNode2.key != null){
                                     String keyN = etcdNode2.key.substring((etcdNameSpace.length()+1)) ;  
                                   nodeMap.put(keyN ,new Node(keyN, etcdNode2.value.getBytes()));
                                   }
                            }
                            
                      }
               }
               else {
                     if(etcdNode.key != null){
                     String keyN = etcdNode.key.substring((etcdNameSpace.length()+1)) ;	 
                     nodeMap.put(keyN, new Node(keyN, etcdNode.value.getBytes()));
                     }
               }
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
    	etcd.put(key,new String(value)).send();
    }

    /**
     * removes the "path" from etcd
     *
     * @param path
     * @throws Exception
     */
    @Override
    public void remove(String path) throws Exception {
    	etcd.deleteDir(path).recursive().send();
    }

    /**
     * deletes the "deletePath" from Etcd and creates the "recordMap" parameters in Etcd in a single transaction.
     *
     * @param recordMap
     * @param deletePath
     * @throws Exception
     */
    @Override
    public void atomicBatchUpdate(Map<String, byte[]> recordMap, String deletePath) throws Exception {
    	
    	
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
            set(currentPath,recordMap.get(path));
        }
    }

    /**
     * creates the "recordMap" parameters in etcd.
     *
     * @param recordMap
     */
    @Override
    public void atomicBatchUpdate(Map<String, byte[]> recordMap) throws Exception {
        atomicBatchUpdate(recordMap, null);
    }

    /**
     * The getter for etcd property
     *
     * @param key
     */
    @Override
    public byte[] get(String key) {
    	
		EtcdKeysResponse response1=null;
		try {
			response1 = etcd.get(PATH_SEPARATOR+etcdNameSpace+key).send().get();
			
		} catch (IOException | EtcdException | EtcdAuthenticationException		| TimeoutException e) {
			log.error(e.getMessage());
		}
        return response1!=null && response1.node != null ? response1.node.value.getBytes() : null;
    }

    /**
     * Registers a TreeCacheListener
     *
     * @param treeCacheListener
     */
   public void addListenerToTreeCache(TreeCacheListener treeCacheListener) {
        //this.cache.getListenable().addListener(treeCacheListener);
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addListenerToPath(final String path, NodeListener listener, long notificationDelay) {
    	 
    	EtcdResponsePromise<EtcdKeysResponse> promise;
    	final TimeDelayedEtcdNodeListener timeDelayedNodeListener = new TimeDelayedEtcdNodeListener(path, listener, notificationDelay);
		try {
			  promise = etcd.get(path).recursive().waitForChange().send();
	    	  promise.addListener(new ResponsePromise.IsSimplePromiseResponseHandler() {
	              @Override
	              public void onResponse(ResponsePromise responsePromise) {
	                  try {
	                      EtcdKeysResponse response = (EtcdKeysResponse) responsePromise.get();
	                      EtcdResponsePromise next = etcd.get(path).recursive().waitForChange(response.node.modifiedIndex + 1).send();
	                      timeDelayedNodeListener.insertEtcdEvent(response);
	                      next.addListener(this);
	                  } catch (Exception e) {
	                      log.error("Error while adding listner for path: "+path);
	                  }
	              }
	    	  });
		} catch (IOException ex) {
			ex.printStackTrace();
			log.debug("ERROR while adding Listener To Path..."+ex.getMessage());
		}
		 
		timeDelayedEtcdNodeListeners.add(timeDelayedNodeListener);
        timeDelayedNodeListener.start(); 
    }

    /**
     * Called by spring in @PreDestroy phase. You shall call this method yourself if you created the object yourself (not using Spring)
     */
    @PreDestroy
    public void close() {
        log.debug("Closing ConfigurationServiceImpl...client...");
        try {
            this.etcd.close();
            log.debug("Closing ConfigurationServiceImpl...connected clients...");
            this.timeDelayedEtcdNodeListeners.forEach(TimeDelayedEtcdNodeListener::stop);
            log.debug("Will sleep for " + Integer.toString(WAIT_BEFORE_CLOSE) + " milisec...");
            Thread.sleep(WAIT_BEFORE_CLOSE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void unregisterAllListeners() {
        this.timeDelayedEtcdNodeListeners.forEach(TimeDelayedEtcdNodeListener::stop);
    }
}
