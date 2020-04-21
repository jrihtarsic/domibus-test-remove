package eu.domibus.api.cluster;

import eu.domibus.api.multitenancy.Domain;

/**
 * @author idragusa
 * @since 4.2
 */
public interface CommandExecutorService {

    /**
     * Execute commands by server and domain (find commands, then execute and delete each command)
     *
     * @param serverName
     * @param domain
     */
    void executeCommands(String serverName, Domain domain);

}
