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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;




//import org.apache.zookeeper.KeeperException;
//import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nokia.cxs.configuration.service.api.ConfigurationService;
import com.nokia.cxs.configuration.service.api.impl.ConfigurationServiceEtcdImpl;

/**
 * @author lbacila
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
public class ConfigurationServiceImplITest2 {
    @Autowired
    private ConfigurationService configurationService;

    /**
     * 
     * @throws Exception
     */
    //@Ignore
    @Test
    public void writeRead() throws Exception {
        writeRead("/sbi/plugins", "plugin1");
    }
    
    /**
     * 
     * @param path
     * @param val
     * @throws Exception
     */
    private void writeRead(final String path, String val) throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        ConfigurationServiceEtcdImpl service = (ConfigurationServiceEtcdImpl) configurationService;
        service.addListenerToTreeCache((client, event) -> {
            if (event != null && event.getData() != null && event.getData().getPath()
                    != null && event.getData().getPath().equals(path))
                countDownLatch.countDown();
        });
        configurationService.set(path, val.getBytes());
        //assertTrue(countDownLatch.await(10000, TimeUnit.MILLISECONDS));
        byte[] valInBytes = configurationService.get(path);
        assertEquals(val, new String(valInBytes));
    }

    /**
     * 
     * @param path
     * @throws Exception
     */
    private void removeRead(final String path) throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        ConfigurationServiceEtcdImpl service = (ConfigurationServiceEtcdImpl) configurationService;
        service.addListenerToTreeCache((client, event) -> {
            if (event != null && event.getData() != null && event.getData().getPath()
                    != null && event.getData().getPath().equals(path))
                countDownLatch.countDown();
        });
        configurationService.remove(path);
        assertTrue(countDownLatch.await(10000, TimeUnit.MILLISECONDS));
        byte[] valInBytes = configurationService.get(path);
        assertNull(valInBytes);
    }

   
  
}
