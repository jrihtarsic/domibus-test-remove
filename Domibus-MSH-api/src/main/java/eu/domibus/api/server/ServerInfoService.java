package eu.domibus.api.server;

/**
 * Helper methods for each type of server (Tomcat, Weblogic, Wildfly)
 *
 * @author Catalin Enache
 * @since 4.0.1
 */
public interface ServerInfoService {

    /**
     * Returns an unique identifier per server/JVM
     * To be used when sending/receiving messages to a Topic in a cluster configuration
     *
     * @return unique server name
     */
    String getUniqueServerName();

    /**
     * Returns the server name
     *
     * @return server name
     */
    String getServerName();
}
