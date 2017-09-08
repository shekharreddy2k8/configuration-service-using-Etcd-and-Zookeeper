package com.shekhar.cxs.configuration.service.api;

/**
 * @author Sangala Shekhar Reddy
 */
public interface NodeListener {
	/**
	 * The callback function which need to be implemented by clients in order to be notified when an event is produced and impacts the pathName
	 * 
	 * @param pathName
	 */
    public void callBack(String pathName);
}
