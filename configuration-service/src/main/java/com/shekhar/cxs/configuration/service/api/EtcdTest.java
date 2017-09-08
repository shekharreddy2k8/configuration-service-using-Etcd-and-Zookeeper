/**
 * 
 */
package com.shekhar.cxs.configuration.service.api;

import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;

/**
 * @author Sangala Shekhar Reddy
 *
 */
public class EtcdTest {

	 public static Map<String, Node> getAllNodes(EtcdClient etcd,String parentPath) {
	        Map<String, Node> nodeMap = new HashMap<>();
	        try {
				getAllNodes(etcd,null,parentPath, nodeMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
	        return nodeMap;
	    }
	
	 public static void getAllNodes(EtcdClient etcd,EtcdNode etcdNode, String directoryPath,Map<String, Node> nodeMap) throws Exception {
	    	
	    	EtcdResponsePromise<EtcdKeysResponse> promise = null;
	        if (etcdNode == null) {
	               promise = etcd.getDir(directoryPath).recursive().send();
	               EtcdKeysResponse etcdKeysResponse= promise.get();
	               List<EtcdNode> nodes=etcdKeysResponse.node.nodes;
	            for (EtcdNode etcdNodeChild : nodes) {
	                getAllNodes(etcd,etcdNodeChild,null,nodeMap);
	               }
	        } else {
	               if (etcdNode.dir) {
	                     promise = etcd.getDir(etcdNode.key).recursive().send();
	                     EtcdKeysResponse keyR= promise.get();
	                     List<EtcdNode> nodes1=keyR.node.nodes;
	                     for(EtcdNode etcdNode2 : nodes1) {
	                            if (etcdNode2.dir) {
	                                   getAllNodes(etcd,etcdNode2,null,nodeMap);
	                            } else {
	                                   if(etcdNode2.key != null){
	                                   nodeMap.put(etcdNode2.key ,new Node(etcdNode2.key, etcdNode2.value.getBytes()));
	                                   }
	                            }
	                            
	                      }
	               }
	               else {
	                     if(etcdNode.key != null){
	                     nodeMap.put(etcdNode.key, new Node(etcdNode.key, etcdNode.value.getBytes()));
	                     }
	               }
	        }
	        //return nodeMap;

	    	
	        
	       /* Map<String, ChildData> currentChildren = cache.getCurrentChildren(parentPath);
	        if (currentChildren != null) {
	            currentChildren.values().stream().forEach(node -> {
	                nodeMap.put(node.getPath(), new Node(node.getPath(), node.getData()));
	                if (node.getStat() != null && node.getStat().getNumChildren() > 0) {
	                    getAllNodes(node.getPath(), nodeMap);
	                }
	            });
	        }*/
	    }
	 
	static EtcdClient etcd=null;
	public static void main(String[] args) throws Exception {

		System.out.println("In Main");
		//EtcdClient etcd = new EtcdClient(URI.create(args[0]));
		
		EtcdClient etcd = new EtcdClient(new URI("http://135.248.176.130:2381"));
		
		//System.out.println(args[0]);
		System.out.println("get all nodes");
		//System.out.println(args[1]);
		System.out.println("test");
		System.out.println(etcd.get("/MAS/sbi/kafka/bootstrap.servers").send().get().node.value);
		//System.out.println("done!");
		etcd.close();
		
		/*EtcdResponsePromise<EtcdKeysResponse> rs=etcd.getDir(args[1]).recursive().send();
		System.out.println("get all nodes ");
		EtcdKeysResponse response1 = rs.get();
		EtcdNode en=response1.node;
		List<EtcdNode> len=en.getNodes();
		for (EtcdNode etcdNode : len) {
			System.out.println("key=" + etcdNode.key + " : value="+etcdNode.value);
		}
		*/

				
		
		/*
		EtcdClient client = new EtcdClient(new URI("http://10.197.53.13:2379"));
		Do p =new Do();
		                                
		                                 
		                 EtcdResponsePromise promise = client.get("/").recursive().waitForChange().send();
		                   EtcdKeysResponse response = (EtcdKeysResponse) promise.get();
		                        EtcdResponsePromise next = client.get("/").recursive().waitForChange(response.etcdIndex + 1).send();
		                   
		                        
		                       // EtcdResponsePromise promise = client.get("/").getPromise();
		                        
		                 promise.addListener(promise1 -> {
	                                try {
	                                    EtcdKeysResponse response = (EtcdKeysResponse) promise.get();
	                                   System.out.println("Waited for: " + response.node.value  + " Key "+response.node.key);
	                                    EtcdResponsePromise next = client.get("/").recursive().waitForChange(response.node.modifiedIndex + 1).send();
	                                    p.print(response.node.value);
	                                    next.addListener(promise1);
	                                } catch (Exception e) {
	                                    e.printStackTrace();
	                                }
	                        });

		                 
		                        promise.addListener(new ResponsePromise.IsSimplePromiseResponseHandler() {
		                            @Override
		                            public void onResponse(ResponsePromise responsePromise) {
		                                try {
		                                    EtcdKeysResponse response = (EtcdKeysResponse) responsePromise.get();
		                                    System.out.println("Waited for: " + response.node.value  + " Key "+response.node.key);
		                                    EtcdResponsePromise next = client.get("/").recursive().waitForChange(response.node.modifiedIndex + 1).send();
		                                    p.print(response.node.value);
		                                    next.addListener(this);
		                                } catch (Exception e) {
		                                    e.printStackTrace();
		                                }
		                            }
		                        });

		                        while(true) {
		                            Thread.sleep(1000);
		                        }
		                */}


		class Print{
		                public void print(String s){
		                                System.out.println("Print: "+s);
		                }
		                
		}

	
	public void getAllNodes()throws Exception{
	
	EtcdClient etcd = new EtcdClient(URI.create("http://localhost:2379"));
	EtcdResponsePromise<EtcdKeysResponse> rs=etcd.getDir("/sai1/ram").recursive().send();
	EtcdKeysResponse response1 = rs.get();
	EtcdNode en=response1.node;
	List<EtcdNode> len=en.getNodes();
	for (EtcdNode etcdNode : len) {
		System.out.println("key=" + etcdNode.key + " : value="+etcdNode.value);
	}
}
	public void getSetEdctValues()throws Exception{
		EtcdClient etcd = new EtcdClient(URI.create("http://localhost:2379"));
		System.out.println(etcd.getVersion());
		EtcdKeyGetRequest req=etcd.get("sai");
		EtcdKeysResponse response1 =etcd.get("sai").send().get();
		System.out.println(response1.node.value); 
		
		EtcdKeysResponse response = etcd.put("foo", "bar").send().get();
		System.out.println(response.node.value);
	}
}
class Do{
	public Do(){
		
	}
	public void print(String s){
		System.out.println("got it: "+s);
	}
}