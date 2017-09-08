/*
 *  Copyright (c) 2015 by Alcatel-Lucent and Motive, Inc. All rights reserved.
 *
 *  CONFIDENTIAL. All rights reserved. This computer program is
 *  protected under Copyright. Recipient is to retain the program in
 *  confidence, and is not permitted to copy, use, distribute, modify or
 *  translate the program without authorization
 *
 *  @author  ccayirog
 *
 *  $Header$
 */
package com.nokia.cxs.configuration.service;

import org.apache.curator.test.TestingServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.nokia.cxs.configuration.service.api.ConfigurationService;
import com.nokia.cxs.configuration.service.api.impl.ConfigurationServiceEtcdImpl;

/**
 * Created by ccayirog on 6/29/2015.
 */
@Configuration
@PropertySource("classpath:bootTest.properties")
public class TestConfiguration {

	/**
	 * 
	 * @return
	 */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * @param zkConnectionString
     * @return
     * @throws Exception
     */
    @Bean(name = "testServer")
    public TestingServer testingServer(@Value("${zookeeper.connectionString}") String zkConnectionString) throws Exception {
    	String zkPort = zkConnectionString.split(",")[0].split(":")[1];
        TestingServer testingServer = new TestingServer(Integer.parseInt(zkPort));
        testingServer.start();
        return testingServer;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    @Bean
    @DependsOn(value = "testServer")
    public ConfigurationService configurationService() throws Exception {
        return new ConfigurationServiceEtcdImpl();
    }


}
