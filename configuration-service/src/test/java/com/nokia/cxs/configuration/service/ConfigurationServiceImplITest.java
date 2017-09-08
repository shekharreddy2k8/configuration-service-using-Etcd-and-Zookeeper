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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.KeeperException;
import org.junit.Ignore;
//import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nokia.cxs.configuration.service.api.ConfigurationService;
import com.nokia.cxs.configuration.service.api.Node;
import com.nokia.cxs.configuration.service.api.impl.ConfigurationServiceEtcdImpl;

/**
 * @author lbacila
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
public class ConfigurationServiceImplITest {
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
        ConfigurationService service = (ConfigurationServiceEtcdImpl) configurationService;
        /*service.addListenerToTreeCache((client, event) -> {
            if (event != null && event.getData() != null && event.getData().getPath()
                    != null && event.getData().getPath().equals(path))
                countDownLatch.countDown();
        });*/
        configurationService.set(path, val.getBytes());
        assertTrue(countDownLatch.await(10000, TimeUnit.MILLISECONDS));
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

    /**
     * 
     * @throws Exception
     */
    //@Ignore
   // @Test
    public void testUpdate() throws Exception {
        writeRead("/sbi/plugins", "plugin1");
        writeRead("/sbi/plugins", "plugin2");
    }

    /**
     * 
     * @throws Exception
     */
    //@Ignore
    //@Test
    public void testRemove() throws Exception {
        writeRead("/sbi/plugins", "plugin1");
        removeRead("/sbi/plugins");
    }

    /**
     * 
     * @throws Exception
     */
    //@Ignore
    //@Test
    public void testRemoveNodeWithChildren() throws Exception {
        writeRead("/sbi/plugin5/conf1", "conf1");
        writeRead("/sbi/plugin5/conf1/subconf1", "subconf1");
        writeRead("/sbi/plugin5/conf2", "conf2");
        writeRead("/sbi/plugin5/conf3", "conf3");
        writeRead("/sbi/plugin5/conf4", "conf4");
        writeRead("/sbi/plugin5/conf5", "conf5");
        writeRead("/sbi/plugin5/conf6", "conf6");
        Exception e = null;
        try {
            removeRead("/sbi/plugin5/conf1");
        } catch (Exception ex) {
            e = ex;
        }
        assertNotNull(e);
        assertTrue(e instanceof KeeperException.NotEmptyException);
    }

    /**
     * 
     * @throws Exception
     */
    //@Ignore
   // @Test
    public void testSubFolders() throws Exception {
        writeRead("/sbi/plugin1/conf1", "conf1");
        writeRead("/sbi/plugin1/conf1/subconf1", "subconf1");
        writeRead("/sbi/plugin1/conf2", "conf2");
        writeRead("/sbi/plugin1/conf3", "conf3");
        writeRead("/sbi/plugin1/conf4", "conf4");
        writeRead("/sbi/plugin2/conf5", "conf5");
        writeRead("/sbi/plugin2/conf6", "conf6");
        Map<String, Node> allSubFolders = configurationService.getAllNodes("/sbi/plugin1");
        assertEquals(5, allSubFolders.size());
        assertEquals("conf1", new String(allSubFolders.get("/sbi/plugin1/conf1").getData()));
        assertEquals("conf2", new String(allSubFolders.get("/sbi/plugin1/conf2").getData()));
        assertEquals("conf3", new String(allSubFolders.get("/sbi/plugin1/conf3").getData()));
        assertEquals("conf4", new String(allSubFolders.get("/sbi/plugin1/conf4").getData()));
        assertEquals("subconf1", new String(allSubFolders.get("/sbi/plugin1/conf1/subconf1").getData()));

    }

    /**
     * 
     * @throws Exception
     */
    //@Ignore
    //@Test
    public void nodeListenerTest() throws Exception {
        final CountDownLatch notificationCountDownLatch = new CountDownLatch(3);
        ConfigurationServiceEtcdImpl service = (ConfigurationServiceEtcdImpl) configurationService;
        service.addListenerToPath("/sbi/plugin10", pathName -> {
            if(notificationCountDownLatch.getCount() == 0){
                fail("more than 3 notification!");
            }
            notificationCountDownLatch.countDown();
        }, 1000L);
        writeRead("/sbi/plugin10/conf1", "conf1");
        writeRead("/sbi/plugin10/conf1", "conf1_updated");
        removeRead("/sbi/plugin10/conf1");
        Thread.sleep(2000L);
        writeRead("/sbi/plugin10/conf2", "conf1");
        writeRead("/sbi/plugin10/conf2", "conf1_updated");
        removeRead("/sbi/plugin10/conf2");
        Thread.sleep(2000L);
        writeRead("/sbi/plugin10/conf3", "conf1");
        writeRead("/sbi/plugin10/conf3", "conf1_updated");
        removeRead("/sbi/plugin10/conf3");
        removeRead("/sbi/plugin10");
        assertTrue(notificationCountDownLatch.await(10000, TimeUnit.MILLISECONDS));
    }

    /**
     * 
     * @throws Exception
     */
    //@Ignore
   // @Test
    public void atomicBatchUpdate() throws Exception {

        Map<String, byte[]> recordMap = new HashMap<>();
        recordMap.put("/sbi/plugin3/conf1", "conf1".getBytes());
        recordMap.put("/sbi/plugin3/conf2", "conf2".getBytes());
        recordMap.put("/sbi/plugin3/conf3", "conf3".getBytes());
        recordMap.put("/sbi/plugin3/conf4", "conf4".getBytes());
        recordMap.put("/sbi/plugin4/conf5", "conf5".getBytes());
        recordMap.put("/sbi/plugin3", "conf6".getBytes());

        final CountDownLatch countDownLatch = new CountDownLatch(6);
        final String path3 = "/sbi/plugin3/conf";
        final String path4 = "/sbi/plugin4/conf";
        ConfigurationServiceEtcdImpl service = (ConfigurationServiceEtcdImpl) configurationService;
        service.addListenerToTreeCache((client, event) -> {
            if (event != null && event.getData() != null && event.getData().getPath()
                    != null && (event.getData().getPath().startsWith(path3) || event.getData().getPath().startsWith(path4)))
                countDownLatch.countDown();
        });
        writeRead("/sbi/plugin3/conf1", "conf1_old");
        configurationService.atomicBatchUpdate(recordMap);
        assertTrue(countDownLatch.await(10000, TimeUnit.MILLISECONDS));
        assertEquals("conf1", new String(configurationService.get("/sbi/plugin3/conf1")));
        assertEquals("conf5", new String(configurationService.get("/sbi/plugin4/conf5")));
    }

    /**
     * 
     * @throws Exception
     */
    //@Ignore
    @Test
    public void atomicBatchUpdateWithDelete() throws Exception {


        writeRead("/sbi/plugin5/conf1/conf1", "conf6");
        writeRead("/sbi/plugin5/conf2/conf2", "conf_value");
        writeRead("/sbi/plugin5/conf3", "conf_value");
        writeRead("/sbi/plugin5/conf4", "conf_value");
        writeRead("/sbi/plugin5/conf5", "conf_value");
        writeRead("/sbi/plugin5/conf6", "conf_value");
        writeRead("/sbi/plugin5/conf7", "conf_value");
        writeRead("/sbi/plugin5/conf8", "conf_value");
        writeRead("/sbi/plugin5/conf9", "conf_value");
        writeRead("/sbi/plugin5/conf10", "conf_value");
        writeRead("/sbi/plugin5/conf11", "conf_value");
        writeRead("/sbi/plugin5/conf12", "conf_value");
        Map<String, Node> allNodes = configurationService.getAllNodes("/sbi/plugin5");
        assertEquals(14, allNodes.size());
        Map<String, byte[]> recordMap = new HashMap<>();
        recordMap.put("/sbi/plugin5/conf1", "new_conf1".getBytes());
        recordMap.put("/sbi/plugin5/conf3", "new_conf3".getBytes());
        recordMap.put("/sbi/plugin5/conf4", "new_conf4".getBytes());
        recordMap.put("/sbi/plugin5/conf5", "new_conf5".getBytes());
        recordMap.put("/sbi/plugin5/conf13", "new_conf13".getBytes());
        final CountDownLatch countDownLatch = new CountDownLatch(15);
        final String path = "/sbi/plugin5/conf";
        ConfigurationServiceEtcdImpl service = (ConfigurationServiceEtcdImpl) configurationService;
        service.addListenerToTreeCache((client, event) -> {
            if (event != null && event.getData() != null && event.getData().getPath()
                    != null && (event.getData().getPath().startsWith(path)))
                countDownLatch.countDown();
        });
        configurationService.atomicBatchUpdate(recordMap, "/sbi/plugin5");
        assertTrue(countDownLatch.await(10000, TimeUnit.MILLISECONDS));
        allNodes = configurationService.getAllNodes("/sbi/plugin5");
        assertEquals(5, allNodes.size());
        assertEquals("new_conf1", new String(configurationService.get("/sbi/plugin5/conf1")));
        assertEquals("new_conf13", new String(configurationService.get("/sbi/plugin5/conf13")));
    }

    /**
     * 
     * @throws Exception
     */
    @Ignore
    public void atomicBatchUpdateWithDelete2() throws Exception {
        writeRead("/mas/notification/destinations/SMP_Workflow/Username", "mysmpuser");
        writeRead("/mas/notification/destinations/SMP_Workflow/Password", "0d98abe9720");
        writeRead("/mas/sftp-scanner/hostname", "ftp.motive.com");
        writeRead("/mas/sftp-scanner/pollIntervalSecs", "30");

        String cdata = "<![CDATA[\n" +
                "Within this Character Data block I can\n" +
                "use double dashes as much as I want (along with <, &, ', and \")\n" +
                "*and* %MyParamEntity; will be expanded to the text\n" +
                "\"Has been expanded\" ... however, I can't use\n" +
                "the CEND sequence (if I need to use it I must escape one of the\n" +
                "brackets or the greater-than sign).\n" +
                "]]>";
        writeRead("/mas/sbi/plugin/csv/xmlcfg",cdata);

        Map<String, byte[]> recordMap = new HashMap<>();
        recordMap.put("/mas/notification/destinations/SMP_Workflow/Username", "mysmpuser1".getBytes());
        recordMap.put("/mas/notification/destinations/SMP_Workflow/Password", "0d98abe9720".getBytes());
        recordMap.put("/mas/sftp-scanner/hostname", "ftp1.motive.com".getBytes());
        recordMap.put("/mas/sftp-scanner/pollIntervalSecs", "30".getBytes());
        cdata = "<![CDATA[\n" +
                "Within this Character Data block I can\n" +
                "use double dashes as much as I want (along with <, &, ', and \")\n" +
                "*and* %MyParamEntity; will be expanded to the text\n" +
                "\"Has been expanded\" ... however, I can't use\n" +
                "the CEND sequence (if I need to use it I must escape one of the\n" +
                "brackets or the greater-than sign).\n" +
                "]]>";
        recordMap.put("/mas/sbi/plugin/csv/xmlcfg", cdata.getBytes());
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        ConfigurationServiceEtcdImpl service = (ConfigurationServiceEtcdImpl) configurationService;
        service.addListenerToPath("/", pathName -> countDownLatch.countDown(),1000);
        configurationService.atomicBatchUpdate(recordMap, "/");

        assertTrue(countDownLatch.await(4000, TimeUnit.MILLISECONDS));
        Map<String, Node> allNodes = configurationService.getAllNodes("/");
        assertEquals(13, allNodes.size());
        assertEquals("ftp1.motive.com", new String(configurationService.get("/mas/sftp-scanner/hostname")));
        assertEquals("mysmpuser1", new String(configurationService.get("/mas/notification/destinations/SMP_Workflow/Username")));
    }

    /**
     * 
     * @throws Exception
     */
	@Ignore
    public void atomicBatchUpdateWithReset() throws Exception {
        writeRead("/sbi/plugin5/conf1/conf1", "conf6");
        writeRead("/sbi/plugin5/conf2/conf2", "conf_value");
        writeRead("/sbi/plugin5/conf3", "conf_value");
        writeRead("/sbi/plugin5/conf4", "conf_value");
        writeRead("/sbi/plugin5/conf5", "conf_value");
        writeRead("/sbi/plugin5/conf6", "conf_value");
        writeRead("/sbi/plugin5/conf7", "conf_value");
        writeRead("/sbi/plugin5/conf8", "conf_value");
        writeRead("/sbi/plugin5/conf9", "conf_value");
        writeRead("/sbi/plugin5/conf10", "conf_value");
        writeRead("/sbi/plugin5/conf11", "conf_value");
        writeRead("/sbi/plugin5/conf12", "conf_value");
        writeRead("/sbi/plugin5", "to_reset_value");
        Map<String, byte[]> recordMap = new HashMap<>();
        recordMap.put("/sbi/plugin5/conf1", "new_conf1".getBytes());
        recordMap.put("/sbi/plugin5/conf3", "new_conf3".getBytes());
        recordMap.put("/sbi/plugin5/conf4", "new_conf4".getBytes());
        recordMap.put("/sbi/plugin5/conf5", "new_conf5".getBytes());
        recordMap.put("/sbi/plugin5/conf13", "new_conf13".getBytes());
        final CountDownLatch countDownLatch = new CountDownLatch(15);
        final String path = "/sbi/plugin5";
        ConfigurationServiceEtcdImpl service = (ConfigurationServiceEtcdImpl) configurationService;
        service.addListenerToTreeCache((client, event) -> {
            if (event != null && event.getData() != null && event.getData().getPath()
                    != null && (event.getData().getPath().startsWith(path)))
                countDownLatch.countDown();
        });
        configurationService.atomicBatchUpdate(recordMap, "/sbi/plugin5");
        assertTrue(countDownLatch.await(10000, TimeUnit.MILLISECONDS));
        Map<String, Node> allNodes = configurationService.getAllNodes("/sbi/plugin5");
        assertEquals(5, allNodes.size());
        assertTrue(new String(configurationService.get("/sbi/plugin5")).isEmpty());
    }
   
}
