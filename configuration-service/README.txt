
The Configuration Service is not designed to be used as a stand alone application. 
The client application has to instantiate the Configuration Service as a Spring bean, 
or by creating an instance of the ConfigurationServiceImpl and inject Zookeeper connection details. 

ConfigurationService depends on shekhar-boot.properties for Zookeeper connection details.
shekhar-boot.properties is therefore expected to be deployed in exactly 2 locations in shekhar:

/opt/shekhar/tomcat/lib/shekhar-boot.properties to make it available to any Tomcat application  


/opt/shekhar/cli-tools/configuration-service-loader/conf/shekhar-boot.properties for configuration-service-loader
     since the configuration-service-loader is a CLI tool, not deployed in Tomcat.
