package com.shekhar.cxs.configuration.service.api;
/**
 * @author Sangala Shekhar Reddy
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.shekhar.cxs.configuration.service.api.impl.ConfigurationServiceEtcdImpl;

@Configuration
@PropertySource("classpath:mas-boot.properties")
public class ConfigurationServiceContext {
	
	/**
	 * 
	 * @return
	 */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    private static ConfigurationService configurationService;

    /**
     * Creates the ConfigurationService bean with properties from mas-boot.properties
     * @return
     */
    @Bean (initMethod="init",destroyMethod="close")
    public ConfigurationService configurationService(){
    	if ( configurationService == null ){
    		//configurationService = new ConfigurationServiceImpl();
    		configurationService = new ConfigurationServiceEtcdImpl();
    	}
    	return configurationService;
    }
    
}
