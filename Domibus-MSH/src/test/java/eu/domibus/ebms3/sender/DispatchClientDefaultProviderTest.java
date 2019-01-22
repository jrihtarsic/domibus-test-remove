package eu.domibus.ebms3.sender;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.proxy.DomibusProxy;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Executor;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */
@RunWith(JMockit.class)
public class DispatchClientDefaultProviderTest {

    @Injectable
    private TLSReader tlsReader;

    @Injectable
    private Executor taskExecutor;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    protected DomibusProxy domibusProxy;

    @Tested
    DispatchClientDefaultProvider dispatchClientDefaultProvider;

    @Test
    public void testSetHttpClientPolicy(@Injectable HTTPClientPolicy httpClientPolicy) {
        String connectionTimeout = "10";
        String receiveTimeout = "60";
        String allowChunking = "true";
        String keepAlive = "true";
        String chunkingThreshold = "100";


        new Expectations() {{
            domibusPropertyProvider.getDomainProperty(DispatchClientDefaultProvider.DOMIBUS_DISPATCHER_CONNECTIONTIMEOUT);
            result = connectionTimeout;

            domibusPropertyProvider.getDomainProperty(DispatchClientDefaultProvider.DOMIBUS_DISPATCHER_RECEIVETIMEOUT);
            result = receiveTimeout;

            domibusPropertyProvider.getDomainProperty(DispatchClientDefaultProvider.DOMIBUS_DISPATCHER_ALLOWCHUNKING);
            result = allowChunking;

            domibusPropertyProvider.getDomainProperty(DispatchClientDefaultProvider.DOMIBUS_DISPATCHER_CHUNKINGTHRESHOLD);
            result = chunkingThreshold;

            domibusPropertyProvider.getDomainProperty(DispatchClientDefaultProvider.DOMIBUS_DISPATCHER_CONNECTION_KEEP_ALIVE);
            result = keepAlive;
        }};


        dispatchClientDefaultProvider.setHttpClientPolicy(httpClientPolicy);

        new Verifications() {{
            httpClientPolicy.setConnectionTimeout(Integer.parseInt(connectionTimeout));
            httpClientPolicy.setReceiveTimeout(Integer.parseInt(receiveTimeout));
            httpClientPolicy.setAllowChunking(Boolean.valueOf(allowChunking));
            httpClientPolicy.setConnection(ConnectionType.KEEP_ALIVE);
            httpClientPolicy.setChunkingThreshold(Integer.parseInt(chunkingThreshold));
        }};
    }
}
