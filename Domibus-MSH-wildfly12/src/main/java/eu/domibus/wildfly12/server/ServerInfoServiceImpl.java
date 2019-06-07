package eu.domibus.wildfly12.server;

import eu.domibus.api.server.ServerInfoService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

/**
 * {@inheritDoc}
 */
@Service
public class ServerInfoServiceImpl implements ServerInfoService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(ServerInfoServiceImpl.class);

    private static final String SERVER_NAME = "jboss.server.name";
    private static final String NODE_NAME = "jboss.node.name";

    @Override
    public String getServerName() {
        final String serverName = System.getenv(SERVER_NAME);
        LOG.debug("serverName={}", serverName);

        return serverName;
    }

    @Override
    public String getNodeName() {
        final String nodeName = System.getProperty(NODE_NAME);

        LOG.debug("nodeName={}", nodeName);
        return nodeName;
    }

    @Override
    public String getHumanReadableServerName() {
        return getNodeName();
    }
}
