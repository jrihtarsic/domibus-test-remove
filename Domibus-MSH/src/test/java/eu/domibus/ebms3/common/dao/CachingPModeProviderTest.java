package eu.domibus.ebms3.common.dao;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.common.dao.ConfigurationDAO;
import eu.domibus.common.dao.ConfigurationRawDAO;
import eu.domibus.common.dao.ProcessDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.common.model.configuration.Role;
import eu.domibus.ebms3.common.model.Ebms3Constants;
import eu.domibus.ebms3.common.model.PartyId;
import eu.domibus.ebms3.common.validators.ConfigurationValidator;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jms.core.JmsOperations;

import javax.persistence.EntityManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Arun Raj
 * @since 3.3
 */
@RunWith(JMockit.class)
public class CachingPModeProviderTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(CachingPModeProviderTest.class);

    private static final String VALID_PMODE_CONFIG_URI = "samplePModes/domibus-configuration-valid.xml";
    private static final String VALID_PMODE_TEST_CONFIG_URI = "samplePModes/domibus-configuration-valid-testservice.xml";
    private static final String PULL_PMODE_CONFIG_URI = "samplePModes/domibus-pmode-with-pull-processes.xml";
    private static final String DEFAULT_MPC_URI = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMpc";
    private static final String ANOTHER_MPC_URI = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/anotherMpc";
    private static final String DEFAULTMPC = "defaultMpc";
    private static final String ANOTHERMPC = "anotherMpc";
    private static final String NONEXISTANTMPC = "NonExistantMpc";

    @Injectable
    ConfigurationDAO configurationDAO;

    @Injectable
    ConfigurationRawDAO configurationRawDAO;

    @Injectable
    EntityManager entityManager;

    @Injectable
    JAXBContext jaxbContextConfig;

    @Injectable
    JMSManager jmsManager;

    @Injectable
    XMLUtil xmlUtil;

    @Injectable
    List<ConfigurationValidator> validators;

    @Injectable
    Configuration configuration;

    @Injectable
    DomainContextProvider domainContextProvider;

    @Injectable
    ProcessPartyExtractorProvider processPartyExtractorProvider;

    @Injectable
    ProcessDao processDao;

    @Tested
    CachingPModeProvider cachingPModeProvider;


    public Configuration loadSamplePModeConfiguration(String samplePModeFileRelativeURI) throws JAXBException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        LOG.debug("Inside sample PMode configuration");
        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream(samplePModeFileRelativeURI);
        JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        configuration = (Configuration) unmarshaller.unmarshal(xmlStream);
        Method m = configuration.getClass().getDeclaredMethod("preparePersist", null);
        m.setAccessible(true);
        m.invoke(configuration);

        return configuration;
    }

    @Test
    public void testIsMpcExistant() throws JAXBException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        configuration = loadSamplePModeConfiguration(VALID_PMODE_CONFIG_URI);

        new Expectations() {{
            cachingPModeProvider.getConfiguration().getMpcs();
            result = configuration.getMpcs();
        }};

        Assert.assertEquals(Boolean.TRUE, cachingPModeProvider.isMpcExistant(DEFAULTMPC.toUpperCase()));
    }

    @Test
    public void testIsMpcExistantNOK() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        configuration = loadSamplePModeConfiguration(VALID_PMODE_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getMpcs();
            result = configuration.getMpcs();
        }};

        Assert.assertEquals(Boolean.FALSE, cachingPModeProvider.isMpcExistant(NONEXISTANTMPC));
    }

    @Test
    public void testGetRetentionDownloadedByMpcName() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        configuration = loadSamplePModeConfiguration(VALID_PMODE_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getMpcs();
            result = configuration.getMpcs();
        }};

        Assert.assertEquals(3, cachingPModeProvider.getRetentionDownloadedByMpcName(ANOTHERMPC.toLowerCase()));
    }

    @Test
    public void testGetRetentionDownloadedByMpcNameNOK() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        configuration = loadSamplePModeConfiguration(VALID_PMODE_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getMpcs();
            result = configuration.getMpcs();
        }};

        Assert.assertEquals(0, cachingPModeProvider.getRetentionDownloadedByMpcName(NONEXISTANTMPC));
    }


    @Test
    public void testGetRetentionUnDownloadedByMpcName() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        configuration = loadSamplePModeConfiguration(VALID_PMODE_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getMpcs();
            result = configuration.getMpcs();
        }};

        Assert.assertEquals(5, cachingPModeProvider.getRetentionUndownloadedByMpcName(ANOTHERMPC.toUpperCase()));
    }

    @Test
    public void testGetRetentionUnDownloadedByMpcNameNOK() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        configuration = loadSamplePModeConfiguration(VALID_PMODE_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getMpcs();
            result = configuration.getMpcs();
        }};

        Assert.assertEquals(-1, cachingPModeProvider.getRetentionUndownloadedByMpcName(NONEXISTANTMPC));
    }

    @Test
    public void testGetMpcURIList() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        configuration = loadSamplePModeConfiguration(VALID_PMODE_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getMpcs();
            result = configuration.getMpcs();
        }};

        List<String> result = cachingPModeProvider.getMpcURIList();
        Assert.assertTrue("URI list should contain DefaultMpc URI", result.contains(DEFAULT_MPC_URI));
        Assert.assertTrue("URI list should contain AnotherMpc URI", result.contains(ANOTHER_MPC_URI));
    }

    @Test
    public void testFindPartyName() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException, EbMS3Exception {
        configuration = loadSamplePModeConfiguration(VALID_PMODE_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getBusinessProcesses().getParties();
            result = configuration.getBusinessProcesses().getParties();
        }};

        Collection<PartyId> partyIdCollection = new ArrayList<>();
        PartyId partyId1 = new PartyId();
        partyId1.setValue("EmptyTestParty");
        partyId1.setType("ABC><123");
        partyIdCollection.add(partyId1);

        try {
            cachingPModeProvider.findPartyName(partyIdCollection);
            Assert.fail("Expected EbMS3Exception due to invalid URI character present!!");
        } catch (Exception e) {
            Assert.assertTrue("Expected EbMS3Exception", e instanceof EbMS3Exception);
        }
    }

    @Test
    public void testRefresh() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        configuration = loadSamplePModeConfiguration(VALID_PMODE_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.configurationDAO.configurationExists();
            result = true;

            cachingPModeProvider.configurationDAO.readEager();
            result = configuration;
        }};

        cachingPModeProvider.refresh();
        Assert.assertEquals(configuration, cachingPModeProvider.getConfiguration());
    }

    @Test
    public void testGetBusinessProcessRoleOk() throws Exception {
        configuration = loadSamplePModeConfiguration(VALID_PMODE_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getBusinessProcesses().getRoles();
            result = configuration.getBusinessProcesses().getRoles();
        }};

        Role expectedRole = new Role();
        expectedRole.setName("defaultInitiatorRole");
        expectedRole.setValue("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator");

        Role role = cachingPModeProvider.getBusinessProcessRole("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator");
        Assert.assertEquals(expectedRole, role);
    }

    @Test
    public void testGetBusinessProcessRoleNOk() throws Exception {
        configuration = loadSamplePModeConfiguration(VALID_PMODE_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getBusinessProcesses().getRoles();
            result = configuration.getBusinessProcesses().getRoles();
        }};

        Role role = cachingPModeProvider.getBusinessProcessRole("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator123");
        Assert.assertNull(role);
    }

    @Test
    public void testRetrievePullProcessBasedOnInitiator() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        configuration = loadSamplePModeConfiguration(PULL_PMODE_CONFIG_URI);
        final Set<Party> parties = configuration.getBusinessProcesses().getParties();
        final Party red_gw = getPartyByName(parties, "red_gw");
        final Party blue_gw = getPartyByName(parties, "blue_gw");
        final Set<Process> processes = configuration.getBusinessProcesses().getProcesses();
        final Collection<Process> filter = Collections2.filter(processes, new Predicate<Process>() {
            @Override
            public boolean apply(Process process) {
                return process.getInitiatorParties().contains(red_gw) && "pull".equals(process.getMepBinding().getName());
            }
        });
        new Expectations() {{
            configurationDAO.configurationExists();
            result = true;
            configurationDAO.readEager();
            result = configuration;
            processDao.findPullProcessesByInitiator(red_gw);
            result = Lists.newArrayList(filter);
            processDao.findPullProcessesByInitiator(blue_gw);
            result = Lists.newArrayList();
        }};
        cachingPModeProvider.init();
        List<Process> pullProcessesByInitiator = cachingPModeProvider.findPullProcessesByInitiator(red_gw);
        Assert.assertEquals(5, pullProcessesByInitiator.size());
        pullProcessesByInitiator = cachingPModeProvider.findPullProcessesByInitiator(blue_gw);
        Assert.assertEquals(0, pullProcessesByInitiator.size());
        new Verifications() {{
            processDao.findPullProcessesByInitiator(red_gw);
            times = 1;
            processDao.findPullProcessesByInitiator(blue_gw);
            times = 1;
        }};
    }

    @Test
    public void testRetrievePullProcessBasedOnPartyNotInInitiator() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        configuration = loadSamplePModeConfiguration(PULL_PMODE_CONFIG_URI);
        final Set<Party> parties = configuration.getBusinessProcesses().getParties();
        final Party white_gw = getPartyByName(parties, "white_gw");
        new Expectations() {{
            configurationDAO.configurationExists();
            result = true;
            configurationDAO.readEager();
            result = configuration;
        }};
        cachingPModeProvider.init();
        List<Process> pullProcessesByInitiator = cachingPModeProvider.findPullProcessesByInitiator(white_gw);
        Assert.assertNotNull(pullProcessesByInitiator);
    }

    @Test
    public void testRetrievePullProcessBasedOnMpc() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        configuration = loadSamplePModeConfiguration(PULL_PMODE_CONFIG_URI);
        final String mpcName = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPCOne";
        final String emptyMpc = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPCTwo";
        new Expectations() {{
            configurationDAO.configurationExists();
            result = true;
            configurationDAO.readEager();
            result = configuration;
            processDao.findPullProcessByMpc(mpcName);
            result = Lists.newArrayList(new Process());
            processDao.findPullProcessByMpc(emptyMpc);
            result = Lists.newArrayList();
        }};
        cachingPModeProvider.init();
        List<Process> pullProcessesByMpc = cachingPModeProvider.findPullProcessByMpc(mpcName);
        Assert.assertEquals(1, pullProcessesByMpc.size());
        pullProcessesByMpc = cachingPModeProvider.findPullProcessByMpc(emptyMpc);
        Assert.assertEquals(0, pullProcessesByMpc.size());
        new Verifications() {{
            processDao.findPullProcessByMpc(mpcName);
            times = 1;
            processDao.findPullProcessByMpc(emptyMpc);
            times = 1;
        }};
    }

    @Test
    public void testFindPartyIdByServiceAndAction() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        // Given
        List<String> expectedList = new ArrayList<>();
        expectedList.add("domibus-blue");
        expectedList.add("domibus-red");
        configuration = loadSamplePModeConfiguration(VALID_PMODE_TEST_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getBusinessProcesses().getProcesses();
            result = configuration.getBusinessProcesses().getProcesses();
        }};

        // When
        List<String> partyIdByServiceAndAction = cachingPModeProvider.findPartyIdByServiceAndAction(Ebms3Constants.TEST_SERVICE, Ebms3Constants.TEST_ACTION);

        // Then
        Assert.assertEquals(expectedList, partyIdByServiceAndAction);
    }


    private Party getPartyByName(Set<Party> parties, final String partyName) {
        final Collection<Party> filter = Collections2.filter(parties, new Predicate<Party>() {
            @Override
            public boolean apply(Party party) {
                return partyName.equals(party.getName());
            }
        });
        return Lists.newArrayList(filter).get(0);
    }

    @Test
    public void testGetPartyIdType() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        // Given
        String partyIdentifier = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:domibus-de";
        configuration = loadSamplePModeConfiguration(VALID_PMODE_TEST_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getBusinessProcesses().getParties();
            result = configuration.getBusinessProcesses().getParties();
        }};

        // When
        String partyIdType = cachingPModeProvider.getPartyIdType(partyIdentifier);

        // Then
        Assert.assertTrue(StringUtils.isEmpty(partyIdType));
    }

    @Test
    public void testGetPartyIdTypeNull() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        // Given
        String partyIdentifier = "empty";

        // When
        String partyIdType = cachingPModeProvider.getPartyIdType(partyIdentifier);

        // Then
        Assert.assertNull(partyIdType);
    }

    @Test
    public void testGetServiceType() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        //Given
        String serviceValue = "bdx:noprocess";
        configuration = loadSamplePModeConfiguration(VALID_PMODE_TEST_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getBusinessProcesses().getServices();
            result = configuration.getBusinessProcesses().getServices();
        }};

        // When
        String serviceType = cachingPModeProvider.getServiceType(serviceValue);

        // Then
        Assert.assertEquals("tc2", serviceType);
    }

    @Test
    public void testGetServiceTypeNull() {
        // Given
        String serviceValue = "serviceValue";

        // When
        String serviceType = cachingPModeProvider.getServiceType(serviceValue);

        // Then
        Assert.assertNull(serviceType);
    }

    @Test
    public void testGetProcessFromService() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        // Given
        configuration = loadSamplePModeConfiguration(VALID_PMODE_TEST_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getBusinessProcesses().getProcesses();
            result = configuration.getBusinessProcesses().getProcesses();
        }};

        // When
        List<Process> processFromService = cachingPModeProvider.getProcessFromService(Ebms3Constants.TEST_SERVICE);

        // Then
        Assert.assertEquals(1, processFromService.size());
        Assert.assertEquals("testService", processFromService.get(0).getName());
    }

    @Test
    public void testGetProcessFromServiceNull() {
        // Given
        String serviceValue = "serviceValue";

        // When
        List<Process> processFromService = cachingPModeProvider.getProcessFromService(serviceValue);

        // Then
        Assert.assertTrue(processFromService.isEmpty());
    }

    @Test
    public void testGetRoleInitiator() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        // Given
        configuration = loadSamplePModeConfiguration(VALID_PMODE_TEST_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getBusinessProcesses().getProcesses();
            result = configuration.getBusinessProcesses().getProcesses();

            cachingPModeProvider.getProcessFromService(Ebms3Constants.TEST_SERVICE);
            result = getTestProcess(configuration.getBusinessProcesses().getProcesses());

        }};

        // When
        String initiator = cachingPModeProvider.getRole("INITIATOR", Ebms3Constants.TEST_SERVICE);

        // Then
        Assert.assertEquals("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator", initiator);
    }

    @Test
    public void testGetRoleResponder() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        // Given
        configuration = loadSamplePModeConfiguration(VALID_PMODE_TEST_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getBusinessProcesses().getProcesses();
            result = configuration.getBusinessProcesses().getProcesses();

            cachingPModeProvider.getProcessFromService(Ebms3Constants.TEST_SERVICE);
            result = getTestProcess(configuration.getBusinessProcesses().getProcesses());

        }};

        // When
        String responder = cachingPModeProvider.getRole("RESPONDER", Ebms3Constants.TEST_SERVICE);

        // Then
        Assert.assertEquals("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder", responder);
    }

    @Test
    public void testGetRoleNull() {
        // Given
        String serviceValue = "serviceValue";

        // When
        String role = cachingPModeProvider.getRole("", serviceValue);

        // Then
        Assert.assertNull(role);
    }

    @Test
    public void testAgreementRef() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JAXBException {
        // Given
        configuration = loadSamplePModeConfiguration(VALID_PMODE_TEST_CONFIG_URI);
        new Expectations() {{
            cachingPModeProvider.getConfiguration().getBusinessProcesses().getProcesses();
            result = configuration.getBusinessProcesses().getProcesses();

            cachingPModeProvider.getProcessFromService(Ebms3Constants.TEST_SERVICE);
            result = getTestProcess(configuration.getBusinessProcesses().getProcesses());

        }};

        // When
        String agreementRef = cachingPModeProvider.getAgreementRef(Ebms3Constants.TEST_SERVICE);

        // Then
        Assert.assertEquals("TestServiceAgreement", agreementRef);
    }

    @Test
    public void testAgreementRefNull() {
        // Given
        String serviceValue = "serviceValue";

        // When
        String agreementRef = cachingPModeProvider.getAgreementRef(serviceValue);

        // Then
        Assert.assertNull(agreementRef);
    }

    private Process getTestProcess(Set<Process> processes) {
        for(Process process : processes) {
            if(process.getName().equals("testService")) {
                return process;
            }
        }
        return null;
    }
}
