package eu.domibus.pmode;

import eu.domibus.AbstractIT;
import eu.domibus.api.pmode.PModeValidationException;
import eu.domibus.api.util.xml.UnmarshallerResult;
import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.common.model.configuration.*;
import eu.domibus.core.message.MessageExchangeConfiguration;
import eu.domibus.core.pmode.ConfigurationDAO;
import eu.domibus.core.pmode.ConfigurationRawDAO;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.web.rest.PModeResource;
import eu.domibus.web.rest.ro.ValidationResponseRO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;


/**
 * This JUNIT implements the Test cases: UploadPMode - 01, UploadPMode - 02, UploadPMode - 03.
 *
 * @author martifp
 * @author Catalin Enache
 */
@DirtiesContext
@Rollback
@Transactional
public class UploadPModeIT extends AbstractIT {

    public static final String SCHEMAS_DIR = "schemas/";
    public static final String DOMIBUS_PMODE_XSD = "domibus-pmode.xsd";

    private static final String BLUE_2_RED_SERVICE1_ACTION1_PMODE_KEY = "blue_gw" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
            "red_gw" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
            "testService1" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
            "tc1Action" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
            "agreement1110" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR + "pushTestcase1tc1Action";

    private static final String PREFIX_MPC_URI = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";
    public static final String SAMPLE_PMODES_DOMIBUS_CONFIGURATION_VALID_XML = "samplePModes/domibus-configuration-valid.xml";


    @Autowired
    PModeResource adminGui;

    @Autowired
    ConfigurationDAO configurationDAO;

    @Autowired()
    @Qualifier("jaxbContextConfig")
    private JAXBContext jaxbContext;

    @Autowired
    XMLUtil xmlUtil;

    @Autowired
    ConfigurationRawDAO configurationRawDAO;

    /**
     * Tests that the PMODE is correctly saved in the DB.
     *
     * @throws IOException
     * @throws XmlProcessingException
     */
    @Test
    public void testSavePModeOk() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-configuration-valid.xml");
        pModeProvider.updatePModes(IOUtils.toByteArray(is), "description");
    }

    /**
     * Tests that the PMode is not saved in the DB because there is a wrong configuration.
     */
    @Test
    public void testSavePModeNOk() throws IOException {
        String pmodeName = "domibus-configuration-xsd-not-compliant.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream("samplePModes/" + pmodeName);

        MultipartFile pModeContent = new MockMultipartFile("wrong-domibus-configuration", pmodeName, "text/xml", IOUtils.toByteArray(is));
        try {
            ValidationResponseRO response = adminGui.uploadPMode(pModeContent, "description");
        } catch (PModeValidationException ex) {
            assertTrue(ex.getMessage().contains("Failed to upload the PMode file due to"));
        }
    }

    private Configuration testUpdatePModes(final byte[] bytes) throws JAXBException {
        final Configuration configuration = (Configuration) this.jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(bytes));
        configurationDAO.updateConfiguration(configuration);
        return configuration;
    }

    /**
     * Tests that a subset of the PMODE file content (given a fixed pModeKey) is correctly stored in the DB.
     * <p>
     * PMODE Key  = Initiator Party: Responder Party: Service name: Action name: Agreement: Test case name
     */
    @Test
    public void testVerifyPModeContent() throws IOException, JAXBException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(SAMPLE_PMODES_DOMIBUS_CONFIGURATION_VALID_XML);
        Configuration configuration = testUpdatePModes(IOUtils.toByteArray(is));
        // Starts to check that the content of the XML file has actually been saved!
        Party receiverParty = pModeProvider.getReceiverParty(BLUE_2_RED_SERVICE1_ACTION1_PMODE_KEY);
        Validate.notNull(receiverParty, "Responder party was not found");
        Party senderParty = pModeProvider.getSenderParty(BLUE_2_RED_SERVICE1_ACTION1_PMODE_KEY);
        Validate.notNull(senderParty, "Initiator party was not found");
        List<String> parties = new ArrayList<>();
        parties.add(receiverParty.getName());
        parties.add(senderParty.getName());

        boolean partyFound = false;
        Iterator<Party> partyIterator = configuration.getBusinessProcesses().getParties().iterator();
        while (!partyFound && partyIterator.hasNext()) {
            Party party = partyIterator.next();
            partyFound = parties.contains(party.getName());
        }
        assertTrue(partyFound);

        Action savedAction = pModeProvider.getAction(BLUE_2_RED_SERVICE1_ACTION1_PMODE_KEY);
        boolean actionFound = false;
        Iterator<Action> actionIterator = configuration.getBusinessProcesses().getActions().iterator();
        while (!actionFound && actionIterator.hasNext()) {
            Action action = actionIterator.next();
            if (action.getName().equals(savedAction.getName())) {
                actionFound = true;
            }
        }
        assertTrue(actionFound);

        Service savedService = pModeProvider.getService(BLUE_2_RED_SERVICE1_ACTION1_PMODE_KEY);
        boolean serviceFound = false;
        Iterator<Service> serviceIterator = configuration.getBusinessProcesses().getServices().iterator();
        while (!serviceFound && serviceIterator.hasNext()) {
            Service service = serviceIterator.next();
            if (service.getName().equals(savedService.getName())) {
                serviceFound = true;
            }
        }
        assertTrue(serviceFound);

        LegConfiguration savedLegConf = pModeProvider.getLegConfiguration(BLUE_2_RED_SERVICE1_ACTION1_PMODE_KEY);
        boolean legConfFound = false;
        Iterator<LegConfiguration> legConfIterator = configuration.getBusinessProcesses().getLegConfigurations().iterator();
        while (!legConfFound && legConfIterator.hasNext()) {
            LegConfiguration legConf = legConfIterator.next();
            if (legConf.getName().equals(savedLegConf.getName())) {
                legConfFound = true;
            }
        }
        assertTrue(legConfFound);

        Agreement savedAgreement = pModeProvider.getAgreement(BLUE_2_RED_SERVICE1_ACTION1_PMODE_KEY);
        boolean agreementFound = false;
        Iterator<Agreement> agreementIterator = configuration.getBusinessProcesses().getAgreements().iterator();
        while (!agreementFound && agreementIterator.hasNext()) {
            Agreement agreement = agreementIterator.next();
            if (agreement.getName().equals(savedAgreement.getName())) {
                agreementFound = true;
            }
        }
        assertTrue(agreementFound);

        List<String> mpcNames = pModeProvider.getMpcList();
        Map<String, Mpc> savedMpcs = new HashMap<>();
        for (String mpcName : mpcNames) {
            Mpc mpc = new Mpc();
            mpc.setName(mpcName);
            mpc.setQualifiedName(PREFIX_MPC_URI + mpcName);
            mpc.setDefault(true);
            mpc.setEnabled(true);
            mpc.setRetentionDownloaded(pModeProvider.getRetentionDownloadedByMpcURI(mpc.getQualifiedName()));
            mpc.setRetentionUndownloaded(pModeProvider.getRetentionUndownloadedByMpcURI(mpc.getQualifiedName()));
            savedMpcs.put(mpcName, mpc);
        }

        for (Mpc mpc : configuration.getMpcs()) {
            Mpc savedMpc = savedMpcs.get(mpc.getName());
            assertNotNull(savedMpc);
            assertEquals(mpc.getName(), savedMpc.getName());
            assertEquals(mpc.getQualifiedName(), savedMpc.getQualifiedName());
            assertEquals(mpc.getRetentionDownloaded(), savedMpc.getRetentionDownloaded());
            assertEquals(mpc.getRetentionUndownloaded(), savedMpc.getRetentionUndownloaded());
        }
    }

    /**
     * Tests that the PMode is not saved in the DB because there is a validation error (maxLength exceeded).
     */
    @Test
    public void testSavePModeValidationError() throws IOException {
        String pmodeName = "domibus-configuration-long-names.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream("samplePModes/" + pmodeName);
        MultipartFile pModeContent = new MockMultipartFile("domibus-configuration-long-names", pmodeName, "text/xml", IOUtils.toByteArray(is));
        try {
            ValidationResponseRO response = adminGui.uploadPMode(pModeContent, "description");
        } catch (PModeValidationException ex) {
            assertTrue(ex.getIssues().size() == 2);
            assertTrue(ex.getIssues().get(0).getMessage().contains("is not facet-valid with respect to maxLength"));
        }
    }

    /**
     * Tests that the PMode is not saved in the DB because there is a validation error (maxSize overflow value).
     */
    @Test
    public void testSavePModeValidationError_MaxSize_Overflow() throws IOException {
        String pmodeName = "domibus-configuration-maxsize-overflow.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream("samplePModes/" + pmodeName);
        MultipartFile pModeContent = new MockMultipartFile("domibus-configuration-maxsize-overflow", pmodeName, "text/xml", IOUtils.toByteArray(is));
        try {
            ValidationResponseRO response = adminGui.uploadPMode(pModeContent, "description");
            fail("exception expected");
        } catch (PModeValidationException ex) {
            assertEquals(0, ex.getIssues().size());
            assertTrue(ex.getMessage().contains("[DOM_003]:Failed to upload the PMode file due to: NumberFormatException: For input string: \"40894464534632746754875696\""));
        }
    }

    /**
     * Tests that the PMode is not saved in the DB because there is a validation error (maxSize overflow value).
     */
    @Test
    public void testSavePModeValidationError_MaxSize_Negative() throws IOException {
        String pmodeName = "domibus-configuration-maxsize-negative.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream("samplePModes/" + pmodeName);
        MultipartFile pModeContent = new MockMultipartFile("domibus-configuration-maxsize-negative", pmodeName, "text/xml", IOUtils.toByteArray(is));
        try {
            ValidationResponseRO response = adminGui.uploadPMode(pModeContent, "description");
            fail("exception expected");
        } catch (PModeValidationException ex) {
            assertEquals(1, ex.getIssues().size());
            assertTrue(ex.getIssues().get(0).getMessage().contains("the maxSize value [-4089446453400] of payload profile [MessageProfile] should be neither negative neither a positive value greater than 9223372036854775807"));
        }
    }


    /**
     * Tests that a PMODE can be serialized/deserialized properly.
     */
    @Test
    public void testVerifyPartyListUpdate() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-configuration-valid.xml");
        byte[] bytes = IOUtils.toByteArray(is);

        InputStream xsdStream = getClass().getClassLoader().getResourceAsStream(SCHEMAS_DIR + DOMIBUS_PMODE_XSD);
        ByteArrayInputStream xmlStream = new ByteArrayInputStream(bytes);

        UnmarshallerResult unmarshallerResult = xmlUtil.unmarshal(true, jaxbContext, xmlStream, xsdStream);
        Configuration configuration = unmarshallerResult.getResult();

        pModeProvider.serializePModeConfiguration(configuration);
    }


    /**
     * Tests that the PMode is not saved in the DB because there is a validation error for duplicate party identifier.
     */
    @Test
    public void testUploadPmodeDuplicateIdentifier() throws IOException {
        String pmodeName = "domibus-pmode-identifier-validation-blue.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream("samplePModes/" + pmodeName);
        MultipartFile pModeContent = new MockMultipartFile("domibus-pmode-identifier-validation-blue", pmodeName, "text/xml", IOUtils.toByteArray(is));
        try {
            ValidationResponseRO response = adminGui.uploadPMode(pModeContent, "description");
            fail("exception expected");
        } catch (PModeValidationException ex) {
            assertTrue(ex.getIssues().get(0).getMessage().contains("Duplicate party identifier [domibus-blue] found"));
        }
    }

    /**
     * Tests that the PMode is not saved in the DB because there is a validation error for duplicate entities.
     */
    @Test
    public void testUploadPmodeDuplicateEntities() throws IOException {
        String pmodeName = "domibus-pmode-duplicate-entities-validation-blue.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream("samplePModes/" + pmodeName);
        MultipartFile pModeContent = new MockMultipartFile("domibus-pmode-duplicate-entities-validation-blue", pmodeName, "text/xml", IOUtils.toByteArray(is));
        try {
            ValidationResponseRO response = adminGui.uploadPMode(pModeContent, "description");
            fail("exception expected");
        } catch (PModeValidationException ex) {
            assertTrue(ex.getIssues().get(0).getMessage().contains("Duplicate unique value [defaultMpc] declared for identity constraint of element \"mpcs\"."));
            assertTrue(ex.getIssues().get(1).getMessage().contains("Duplicate unique value [http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC] declared for identity constraint of element \"mpcs\"."));
            assertTrue(ex.getIssues().get(2).getMessage().contains("Duplicate unique value [defaultInitiatorRole] declared for identity constraint of element \"roles\"."));
            assertTrue(ex.getIssues().get(3).getMessage().contains("Duplicate unique value [partyTypeUrn] declared for identity constraint of element \"partyIdTypes\"."));
            assertTrue(ex.getIssues().get(4).getMessage().contains("Duplicate unique value [urn:oasis:names:tc:ebcore:partyid-type:unregistered] declared for identity constraint of element \"partyIdTypes\"."));
            assertTrue(ex.getIssues().get(5).getMessage().contains("Duplicate unique value [red_gw] declared for identity constraint of element \"parties\"."));
            assertTrue(ex.getIssues().get(6).getMessage().contains("Duplicate unique value [oneway] declared for identity constraint of element \"meps\"."));
        }
    }

    /**
     * Tests that the PMode is not saved in the DB because there is a type validation error for AS entities.
     */
    @Test
    public void testUploadPmodeAs4BooleanEntities() throws IOException {
        String pmodeName = "domibus-pmode-AS4-entities-validation-blue.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream("samplePModes/" + pmodeName);
        MultipartFile pModeContent = new MockMultipartFile("domibus-pmode-AS4-entities-validation-blue", pmodeName, "text/xml", IOUtils.toByteArray(is));
        try {
            ValidationResponseRO response = adminGui.uploadPMode(pModeContent, "description");
            fail("exception expected");
        } catch (PModeValidationException ex) {
            assertEquals(4, ex.getIssues().size());
            assertTrue(ex.getIssues().get(0).getMessage().contains("'true123' is not a valid value for 'boolean'."));
            assertTrue(ex.getIssues().get(1).getMessage().contains("The value 'true123' of attribute 'duplicateDetection' on element 'receptionAwareness' is not valid with respect to its type, 'boolean'."));
            assertTrue(ex.getIssues().get(2).getMessage().contains(" 'true11' is not a valid value for 'boolean'."));
            assertTrue(ex.getIssues().get(3).getMessage().contains("The value 'true11' of attribute 'nonRepudiation' on element 'reliability' is not valid with respect to its type, 'boolean'."));
        }
    }

    /**
     * Tests that the PMode is not saved in the DB because there is a validation error for URI attributes exceeds-maxlength.
     */
    @Test
    public void testUploadPmode_URI_attributes_Exceeds_MaxLength() throws IOException {
        String pmodeName = "domibus-pmode-URI-attributes-exceeds-maxlength.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream("samplePModes/" + pmodeName);
        MultipartFile pModeContent = new MockMultipartFile("domibus-pmode-URI-attributes-exceeds-maxlength", pmodeName, "text/xml", IOUtils.toByteArray(is));
        try {
            ValidationResponseRO response = adminGui.uploadPMode(pModeContent, "description");
            fail("exception expected");
        } catch (PModeValidationException ex) {
            assertEquals(4, ex.getIssues().size());
            assertTrue(ex.getIssues().get(0).getMessage().contains("is not facet-valid with respect to maxLength '1024' for type 'max1024-anyURI"));
            assertTrue(ex.getIssues().get(1).getMessage().contains("attribute 'value' on element 'partyIdType' is not valid with respect to its type, 'max1024-anyURI"));
            assertTrue(ex.getIssues().get(2).getMessage().contains("is not facet-valid with respect to maxLength '1024' for type 'max1024-anyURI"));
            assertTrue(ex.getIssues().get(3).getMessage().contains("attribute 'endpoint' on element 'party' is not valid with respect to its type, 'max1024-anyURI"));
        }
    }
}
