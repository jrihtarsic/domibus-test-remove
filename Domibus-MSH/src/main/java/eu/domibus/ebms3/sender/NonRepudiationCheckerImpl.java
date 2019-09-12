package eu.domibus.ebms3.sender;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


/**
 * Created by feriaad on 15/02/2016.
 */
@Service
public class NonRepudiationCheckerImpl implements NonRepudiationChecker {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(NonRepudiationCheckerImpl.class);

    private static final String XPATH_EXPRESSION_STRING = ".//*[local-name() = 'Reference']/@URI  | .//*[local-name() = 'Reference']/*[local-name() = 'DigestValue']";
    private final XPath xPath = XPathFactory.newInstance().newXPath();

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public NodeList getNonRepudiationNodeList(final Node securityInfo) throws EbMS3Exception {
        NodeList nodes = null;
        try {
            //TODO optimize this by creating an XPathExpression using a specified expression String and re-use it
            nodes = (NodeList) xPath.evaluate(NonRepudiationCheckerImpl.XPATH_EXPRESSION_STRING, securityInfo, XPathConstants.NODESET);
        } catch (final XPathExpressionException e) {
            assert false;
            // due to the fact that we use a static expression this can never occur.
            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_001, "XPath problem occurred", e);
        }

        if (nodes == null) {
            EbMS3Exception e = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0302, "No Reference Data found in either security header or nonrepudiationinformation", null, null);
            e.setMshRole(MSHRole.SENDING);
            throw e;
        }

        return nodes;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean compareUnorderedReferenceNodeLists(final NodeList referencesFromSecurityHeader, final NodeList referencesFromNonRepudiationInformation) {
        if (referencesFromSecurityHeader.getLength() != referencesFromNonRepudiationInformation.getLength()) {
            return false;
        }
        boolean found = false;
        for (int i = 0; i < referencesFromSecurityHeader.getLength(); ++i) {
            final String referenceFromSecurityHeader = referencesFromSecurityHeader.item(i).getTextContent();
            for (int j = 0; j < referencesFromNonRepudiationInformation.getLength(); ++j) {
                if (referenceFromSecurityHeader.equals(referencesFromNonRepudiationInformation.item(j).getTextContent())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                LOG.error("The reference [{}] from the request Signature could not be found in the response NonRepudiationInformation", referenceFromSecurityHeader);
                return false;
            }
            found = false;
        }

        return true;
    }
}
