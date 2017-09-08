# configuration-service-using-Etcd-and-Zookeeper
The Configuration Service is not designed to be used as a stand alone application.  The client application has to instantiate the Configuration Service as a Spring bean,  or by creating an instance of the ConfigurationServiceImpl and inject Zookeeper connection details. 
