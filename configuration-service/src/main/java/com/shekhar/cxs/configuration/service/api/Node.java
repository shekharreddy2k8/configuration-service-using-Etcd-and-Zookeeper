package com.shekhar.cxs.configuration.service.api;
/**
 * @author Sangala Shekhar Reddy
 */
public class Node {

    private final String path;
    private final byte[] data;

    /**
     * 
     * @param path
     * @param data
     */
    public Node(String path, byte[] data) {
        this.path = path;
        this.data = data;
    }

    /**
     * getter for path
     * 
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * getter for Data
     * 
     * @return
     */
    public byte[] getData() {
        return data;
    }
}
