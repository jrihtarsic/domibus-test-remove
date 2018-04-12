package eu.domibus.ebms3.receiver;

import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.ebms3.common.model.MessageInfo;
import eu.domibus.ebms3.common.model.MessageType;
import eu.domibus.ebms3.sender.DispatchClientDefaultProvider;
import eu.domibus.ebms3.sender.MSHDispatcher;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.pki.CertificateService;
import eu.domibus.pki.DomibusCertificateException;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.security.wss4j.CXFRequestData;
import org.apache.cxf.ws.security.wss4j.StaxSerializer;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.str.EncryptedKeySTRParser;
import org.apache.wss4j.dom.str.STRParserParameters;
import org.apache.wss4j.dom.str.STRParserResult;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;

/**
 * This interceptor is responsible of the trust of an incoming messages.
 * Useful info on this topic are here: http://tldp.org/HOWTO/SSL-Certificates-HOWTO/x64.html
 *
 * @author Martini Federico
 * @since 3.3
 */
public class TrustSenderInterceptor extends WSS4JInInterceptor {

    protected static final String DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING = "domibus.sender.trust.validation.onreceiving";

    protected static final String DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING = "domibus.sender.certificate.validation.onreceiving";

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TrustSenderInterceptor.class);

    public static final QName KEYINFO = new QName("http://www.w3.org/2000/09/xmldsig#", "KeyInfo");

    protected Properties securityEncryptionProp;

    @Autowired
    @Qualifier("domibusProperties")
    private Properties domibusProperties;

    @Qualifier("jaxbContextEBMS")
    @Autowired
    protected JAXBContext jaxbContext;

    @Autowired
    private CertificateService certificateService;

    public TrustSenderInterceptor() {
        super(false);
    }

    public void setSecurityEncryptionProp(Properties securityEncryptionProp) {
        this.securityEncryptionProp = securityEncryptionProp;
    }

    /**
     * Intercepts a message to verify that the sender is trusted.
     *
     * There will be two validations:
     *      a) the sender certificate is valid and not revoked and
     *      b) the sender party name is included in the CN of the certificate
     *
     * @param message the incoming CXF soap message to handle
     */
    @Override
    public void handleMessage(final SoapMessage message) throws Fault {

        String messageId = (String) message.getExchange().get(MessageInfo.MESSAGE_ID_CONTEXT_PROPERTY);
        if (!isMessageSecured(message)) {
            LOG.info("Message does not contain security info ==> skipping sender trust verification.");
            return;
        }

        boolean isPullMessage = false;
        MessageType messageType = (MessageType) message.get(MSHDispatcher.MESSAGE_TYPE_IN);
        if (messageType != null && messageType.equals(MessageType.SIGNAL_MESSAGE)) {
            LOG.info("PULL Signal Message");
            isPullMessage = true;
        }

        LOG.info("Validating sender certificate");
        String senderPartyName;
        if (isPullMessage) {
            senderPartyName = getReceiverPartyName(message);
        } else {
            senderPartyName = getSenderPartyName(message);
        }
        X509Certificate certificate = getSenderCertificate(message);
        if (!checkSenderPartyTrust(certificate, senderPartyName, messageId, isPullMessage)) {
            EbMS3Exception ebMS3Ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0101, "Sender [" + senderPartyName + "] is not trusted", messageId, null);
            ebMS3Ex.setMshRole(MSHRole.RECEIVING);
            throw new Fault(ebMS3Ex);
        }

        if (!checkCertificateValidity(certificate, senderPartyName, isPullMessage)) {
            EbMS3Exception ebMS3Ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0101, "Sender [" + senderPartyName + "] certificate is not valid or has been revoked", messageId, null);
            ebMS3Ex.setMshRole(MSHRole.RECEIVING);
            throw new Fault(ebMS3Ex);
        }
    }

    protected Boolean checkCertificateValidity(X509Certificate certificate, String sender, boolean isPullMessage) {
        if (Boolean.parseBoolean(domibusProperties.getProperty(DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING, "true"))) {
            try {
                if (!certificateService.isCertificateValid(certificate)) {
                    LOG.error("Cannot receive message: sender certificate is not valid or it has been revoked [" + sender + "]");
                    return false;
                }
                if (isPullMessage) {
                    LOG.info("[Pulling] - Sender certificate exists and is valid [" + sender + "]");
                } else {
                    LOG.info("Sender certificate exists and is valid [" + sender + "]");
                }
            } catch (DomibusCertificateException dce) {
                LOG.error("Could not verify if the certificate chain is valid for alias " + sender, dce);
                return false;
            }
        }
        return true;
    }

    protected Boolean checkSenderPartyTrust(X509Certificate certificate, String sender, String messageId, boolean isPullMessage) {
        if (!Boolean.parseBoolean(domibusProperties.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING, "false"))) {
            LOG.debug("Sender alias verification is disabled");
            return true;
        }

        LOG.info("Verifying sender trust");
        if(certificate != null && org.apache.commons.lang3.StringUtils.containsIgnoreCase(certificate.getSubjectDN().getName(), sender) ) {
            if (isPullMessage) {
                LOG.info("[Pulling] - Sender [" + sender + "] is trusted.");
            } else {
                LOG.info("Sender [" + sender + "] is trusted.");
            }
            return true;
        }
        if (isPullMessage) {
            LOG.error("[Pulling] - Sender [" + sender + "] is not trusted. To disable this check, set the property " + DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING + " to false.");
        } else {
            LOG.error("Sender [" + sender + "] is not trusted. To disable this check, set the property " + DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING + " to false.");
        }
        return false;
    }

    private boolean isMessageSecured(SoapMessage msg) {
        try {
            return (getSecurityHeader(msg) == null) ? false : true;
        } catch (Exception ex) {
            LOG.error("Error while getting security info", ex);
            return false;
        }
    }

    private Element getSecurityHeader(SoapMessage msg) throws SOAPException, WSSecurityException {

        SOAPMessage doc = msg.getContent(SOAPMessage.class);
        return WSSecurityUtil.getSecurityHeader(doc.getSOAPHeader(), null, true);
    }

    private String getSenderPartyName(SoapMessage message) {
        String pmodeKey = (String) message.get(DispatchClientDefaultProvider.PMODE_KEY_CONTEXT_PROPERTY);
        List<String> contents = StringUtils.getParts(pmodeKey, ":");
        return contents.get(0);
    }


    private String getReceiverPartyName(SoapMessage message) {
        String pmodeKey = (String) message.get(DispatchClientDefaultProvider.PMODE_KEY_CONTEXT_PROPERTY);
        List<String> contents = StringUtils.getParts(pmodeKey, ":");
        return contents.get(1);
    }

    protected X509Certificate getSenderCertificate(SoapMessage msg)  {
        boolean utWithCallbacks = MessageUtils.getContextualBoolean(msg, "ws-security.validate.token", true);
        super.translateProperties(msg);
        CXFRequestData requestData = new CXFRequestData();
        WSSConfig config = (WSSConfig) msg.getContextualProperty(WSSConfig.class.getName());
        WSSecurityEngine engine;
        if (config != null) {
            engine = new WSSecurityEngine();
            engine.setWssConfig(config);
        } else {
            engine = super.getSecurityEngine(utWithCallbacks);
            if (engine == null) {
                engine = new WSSecurityEngine();
            }
            config = engine.getWssConfig();
        }

        requestData.setWssConfig(config);
        requestData.setEncryptionSerializer(new StaxSerializer());

        SoapVersion version = msg.getVersion();
        SAAJInInterceptor.INSTANCE.handleMessage(msg);
        try {
            requestData.setMsgContext(msg);
            decodeAlgorithmSuite(requestData);
            Crypto secCrypto = CryptoFactory.getInstance(securityEncryptionProp);
            requestData.setDecCrypto(secCrypto);
            // extract certificate from KeyInfo
            X509Certificate cert = getCertificateFromKeyInfo(requestData, getSecurityHeader(msg));

            if (cert == null) { // check for certificate embedded in the message under BinarySecurityToken tag
                LOG.info("Checking for message embedded certificate in the BinarySecurityToken tag");
                cert = getCertificateFromBinarySecurityToken(getSecurityHeader(msg));
            }
            if(cert == null) {
                throw new SoapFault("CertificateException: Could not extract the certificate for validation", version.getSender());
            }
            return cert;
        } catch (CertificateException certEx) {
            throw new SoapFault("CertificateException", certEx, version.getSender());
        } catch (WSSecurityException wssEx) {
            throw new SoapFault("WSSecurityException", wssEx, version.getSender());
        } catch (SOAPException soapEx) {
            throw new SoapFault("SOAPException", soapEx, version.getSender());
        }
    }

    protected X509Certificate getCertificateFromBinarySecurityToken(Element securityHeader) throws WSSecurityException, CertificateException {

        NodeList binarySecurityTokenElement = securityHeader.getElementsByTagName("wsse:BinarySecurityToken");
        if(binarySecurityTokenElement == null || binarySecurityTokenElement.item(0) == null)
            return null;

        Element binarySecurityTokenTag = (Element)binarySecurityTokenElement.item(0);

        StringBuffer buf = new StringBuffer();
        NodeList list = binarySecurityTokenTag.getChildNodes();
        boolean found = false;
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                buf.append(node.getNodeValue());
                found = true;
            }
        }
        String certString = buf.toString();

        if(!found ||  certString.isEmpty() ) {
            return null;
        }

        String certStr = ( "-----BEGIN CERTIFICATE-----\n" +  certString + "\n-----END CERTIFICATE-----\n" );
        InputStream in = new ByteArrayInputStream(certStr.getBytes());
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate)certFactory.generateCertificate(in);

        return cert;
    }


    private X509Certificate getCertificateFromKeyInfo(CXFRequestData data, Element securityHeader) throws WSSecurityException {
        X509Certificate[] certs;

        EncryptedKeySTRParser decryptedBytes;
        Element secTokenRef = getSecTokenRef(securityHeader);
        /* CXF class which has to be initialized in order to parse the Security token reference */
        STRParserParameters encryptedEphemeralKey1 = new STRParserParameters();
        data.setWsDocInfo(new WSDocInfo(securityHeader.getOwnerDocument()));
        encryptedEphemeralKey1.setData(data);
        encryptedEphemeralKey1.setStrElement(secTokenRef);
        decryptedBytes = new EncryptedKeySTRParser();
        /* This Apache CXF call will look for a certificate in the Truststore whose Subject Key Identifier bytes matches the <wsse:SecurityTokenReference><wsse:KeyIdentifier> bytes */
        STRParserResult refList = decryptedBytes.parseSecurityTokenReference(encryptedEphemeralKey1);
        certs = refList.getCertificates();

        if (certs == null || certs.length < 1) {
            LOG.warn("No certificate found");
            return null;
        }
        return certs[0];
    }

    private static Element getSecTokenRef(Element soapSecurityHeader) throws WSSecurityException {

        for (Node currentChild = soapSecurityHeader.getFirstChild(); currentChild != null; currentChild = currentChild.getNextSibling()) {
            if (WSConstants.SIGNATURE.getLocalPart().equals(currentChild.getLocalName()) && WSConstants.SIGNATURE.getNamespaceURI().equals(currentChild.getNamespaceURI())) {
                Element signatureEl = (Element) currentChild;
                for (Node innerCurrentChild = signatureEl.getFirstChild(); innerCurrentChild != null; innerCurrentChild = innerCurrentChild.getNextSibling()) {
                    if (KEYINFO.getLocalPart().equals(innerCurrentChild.getLocalName()) && KEYINFO.getNamespaceURI().equals(innerCurrentChild.getNamespaceURI())) {
                        return (Element) innerCurrentChild.getFirstChild();
                    }
                }
            }
        }
        return null;
    }

}


