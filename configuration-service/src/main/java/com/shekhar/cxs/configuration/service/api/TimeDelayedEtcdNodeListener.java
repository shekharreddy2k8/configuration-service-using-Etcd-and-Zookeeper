package com.shekhar.cxs.configuration.service.api;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import mousio.etcd4j.responses.EtcdKeysResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Sangala Shekhar Reddy
 */
public class TimeDelayedEtcdNodeListener {

    private static final Logger log = LoggerFactory.getLogger(TimeDelayedEtcdNodeListener.class);
    private long notificationDelay;
    public static final long QUEUE_TIMEOUT = 10000;
    private DelayQueue<DelayElement> queue = new DelayQueue<DelayElement>();
    private boolean working = true;
    private String pathName;
    private NodeListener nodeListener;

    /**
     * 
     * @param pathName
     * @param nodeListener
     * @param notificationDelay
     */
    public TimeDelayedEtcdNodeListener(String pathName, NodeListener nodeListener, long notificationDelay) {
        this.pathName = pathName;
        this.nodeListener = nodeListener;
        this.notificationDelay = notificationDelay;
    }

    private DelayElement currentDelayElement;

    /**
     * 
     */
    public void start() {
        Thread thread = new Thread(() -> {
            while (working) {
                try {
                    if (queue.poll(QUEUE_TIMEOUT, TimeUnit.MILLISECONDS) != null) {
                        nodeListener.callBack(pathName);
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
        thread.start();
    }

    /**
     * 
     * @param EtcdKeysResponse
     */
    public synchronized void insertEtcdEvent(EtcdKeysResponse res) {
        if (currentDelayElement != null) {
            queue.remove(currentDelayElement);
        }
        currentDelayElement = new DelayElement(res, notificationDelay);
        queue.put(currentDelayElement);
    }

    private class DelayElement implements Delayed {
        private long expiryTime;
        private EtcdKeysResponse res;
        

        /**
         * 
         * @param element
         * @param delay
         */
        public DelayElement(EtcdKeysResponse res, long delay) {
        	this.res=res;
            this.expiryTime = System.currentTimeMillis() + delay;
        }

        /**
         * 
         */
        @Override
        public long getDelay(TimeUnit timeUnit) {
            long diff = expiryTime - System.currentTimeMillis();
            return timeUnit.convert(diff, TimeUnit.MILLISECONDS);
        }

        /**
         * 
         */
        @Override
        public int compareTo(Delayed o) {
            if (this.expiryTime < ((DelayElement) o).expiryTime) {
                return -1;
            }
            if (this.expiryTime > ((DelayElement) o).expiryTime) {
                return 1;
            }
            return 0;
        }

        /**
         * 
         */
        @Override
        public String toString() {
            return res + ": " + expiryTime;
        }
    }

    /**
     * 
     */
    public void stop() {
    	log.debug("TimeDelayedEtcdNodeListener stop() thread...");
        this.working = false;
    }

}
