/*
 * Copyright 2015 e-CODEX Project
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl5
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.domibus.ebms3.sender;

import com.codahale.metrics.Timer;
import com.google.common.base.Strings;
import eu.domibus.api.metrics.Metrics;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.neethi.Policy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import java.net.ConnectException;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Christian Koch, Stefan Mueller
 * @Since 3.0
 */
@Service
public class MSHDispatcher {


    public static final String PMODE_KEY_CONTEXT_PROPERTY = "PMODE_KEY_CONTEXT_PROPERTY";
    public static final String ASYMMETRIC_SIG_ALGO_PROPERTY = "ASYMMETRIC_SIG_ALGO_PROPERTY";
    public static final String MESSAGE_TYPE_IN = "MESSAGE_TYPE";
    public static final String MESSAGE_TYPE_OUT = "MESSAGE_TYPE_OUT";
    public static final String MESSAGE_ID = "MESSAGE_ID";
    public static final QName SERVICE_NAME = new QName("http://domibus.eu", "msh-dispatch-service");
    public static final QName PORT_NAME = new QName("http://domibus.eu", "msh-dispatch");

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MSHDispatcher.class);

    Dispatch<SOAPMessage> dispatch;

    @Autowired
    @Qualifier("taskExecutor")
    Executor executor;

    @Autowired
    private TLSReader tlsReader;

    @Autowired
    @Qualifier("domibusProperties")
    private Properties domibusProperties;

    @Transactional(propagation = Propagation.MANDATORY)
    public SOAPMessage dispatch(final SOAPMessage soapMessage, String endpoint, final Policy policy, final LegConfiguration legConfiguration, final String pModeKey) throws EbMS3Exception {
        final Timer.Context createDispatcherContext = Metrics.METRIC_REGISTRY.timer(name(MSHDispatcher.class, "createWSServiceDispatcher")).time();
        final Dispatch<SOAPMessage> dispatch = createWSServiceDispatcher(endpoint);//service.createDispatch(PORT_NAME, SOAPMessage.class, javax.xml.ws.Service.Mode.MESSAGE);
        createDispatcherContext.stop();


//        dispatch.getRequestContext().put("thread.local.request.context", "true");
        dispatch.getRequestContext().put(PolicyConstants.POLICY_OVERRIDE, policy);
        dispatch.getRequestContext().put(ASYMMETRIC_SIG_ALGO_PROPERTY, legConfiguration.getSecurity().getSignatureMethod().getAlgorithm());
        dispatch.getRequestContext().put(PMODE_KEY_CONTEXT_PROPERTY, pModeKey);

        final Timer.Context getClientContext = Metrics.METRIC_REGISTRY.timer(name(MSHDispatcher.class, "getClient")).time();
        final Client client = ((DispatchImpl<SOAPMessage>) dispatch).getClient();
        getClientContext.stop();

        final Timer.Context prepareClientContext = Metrics.METRIC_REGISTRY.timer(name(MSHDispatcher.class, "prepareClient")).time();
        final HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        final HTTPClientPolicy httpClientPolicy = httpConduit.getClient();
        httpConduit.setClient(httpClientPolicy);
        //ConnectionTimeOut - Specifies the amount of time, in milliseconds, that the consumer will attempt to establish a connection before it times out. 0 is infinite.
        int connectionTimeout = Integer.parseInt(domibusProperties.getProperty("domibus.dispatcher.connectionTimeout", "120000"));
        httpClientPolicy.setConnectionTimeout(connectionTimeout);
        //ReceiveTimeOut - Specifies the amount of time, in milliseconds, that the consumer will wait for a response before it times out. 0 is infinite.
        int receiveTimeout = Integer.parseInt(domibusProperties.getProperty("domibus.dispatcher.receiveTimeout", "120000"));
        httpClientPolicy.setReceiveTimeout(receiveTimeout);
        httpClientPolicy.setAllowChunking(Boolean.valueOf(domibusProperties.getProperty("domibus.dispatcher.allowChunking", "false")));



        if (endpoint.startsWith("https://")) {
            final TLSClientParameters params = tlsReader.getTlsClientParameters();
            if (params != null) {
                httpConduit.setTlsClientParameters(params);
            }
        }
        final SOAPMessage result;

        String useProxy = domibusProperties.getProperty("domibus.proxy.enabled", "false");
        Boolean useProxyBool = Boolean.parseBoolean(useProxy);
        if (useProxyBool) {
            LOG.info("Usage of Proxy required");
            configureProxy(httpClientPolicy, httpConduit);
        } else {
            LOG.info("No proxy configured");
        }
        prepareClientContext.stop();

        final Timer.Context invokeContext = Metrics.METRIC_REGISTRY.timer(name(MSHDispatcher.class, "dispatch.invoke")).time();
        try {
            result = dispatch.invoke(soapMessage);
        } catch (final WebServiceException e) {
            Exception exception = e;
            if (e.getCause() instanceof ConnectException) {
                exception = new WebServiceException("Error dispatching message to [" + endpoint + "]: possible reason is that the receiver is not available", e);
            }
            EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0005, "Error dispatching message to " + endpoint, null, exception);
            ex.setMshRole(MSHRole.SENDING);
            throw ex;
        } finally {
            invokeContext.stop();
        }

        return result;
    }

    //create a pool of Dispatch and associate it to an endpoint
    protected Dispatch<SOAPMessage> createWSServiceDispatcher(String endpoint) {
        if(dispatch == null) {
            final javax.xml.ws.Service service = javax.xml.ws.Service.create(SERVICE_NAME);
            service.setExecutor(executor);
            service.addPort(PORT_NAME, SOAPBinding.SOAP12HTTP_BINDING, endpoint);
            dispatch = service.createDispatch(PORT_NAME, SOAPMessage.class, javax.xml.ws.Service.Mode.MESSAGE);
        }
        return dispatch;

    }

    protected void configureProxy(final HTTPClientPolicy httpClientPolicy, HTTPConduit httpConduit) {
        String httpProxyHost = domibusProperties.getProperty("domibus.proxy.http.host");
        String httpProxyPort = domibusProperties.getProperty("domibus.proxy.http.port");
        String httpProxyUser = domibusProperties.getProperty("domibus.proxy.user");
        String httpProxyPassword = domibusProperties.getProperty("domibus.proxy.password");
        String httpNonProxyHosts = domibusProperties.getProperty("domibus.proxy.nonProxyHosts");
        if (!Strings.isNullOrEmpty(httpProxyHost) && !Strings.isNullOrEmpty(httpProxyPort)) {
            httpClientPolicy.setProxyServer(httpProxyHost);
            httpClientPolicy.setProxyServerPort(Integer.valueOf(httpProxyPort));
            httpClientPolicy.setProxyServerType(org.apache.cxf.transports.http.configuration.ProxyServerType.HTTP);
        }
        if (!Strings.isNullOrEmpty(httpProxyHost)) {
            httpClientPolicy.setNonProxyHosts(httpNonProxyHosts);
        }
        if (!Strings.isNullOrEmpty(httpProxyUser) && !Strings.isNullOrEmpty(httpProxyPassword)) {
            ProxyAuthorizationPolicy policy = new ProxyAuthorizationPolicy();
            policy.setUserName(httpProxyUser);
            policy.setPassword(httpProxyPassword);
            httpConduit.setProxyAuthorization(policy);
        }
    }

    private void warnOutput(String message) {
        LOG.warn("\n\n\n");
        LOG.warn("**************** WARNING **************** WARNING **************** WARNING **************** ");
        LOG.warn(message);
        LOG.warn("*******************************************************************************************\n\n\n");
    }
}

