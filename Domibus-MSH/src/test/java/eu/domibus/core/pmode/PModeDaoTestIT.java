package eu.domibus.core.pmode;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.xml.UnmarshallerResult;
import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.common.dao.*;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.ConfigurationRaw;
import eu.domibus.configuration.DefaultDomibusConfigurationService;
import eu.domibus.core.crypto.DomibusPropertyProviderImpl;
import eu.domibus.core.multitenancy.DomainContextProviderImpl;
import eu.domibus.core.multitenancy.DomainServiceImpl;
import eu.domibus.core.multitenancy.dao.DomainDao;
import eu.domibus.core.multitenancy.dao.DomainDaoImpl;
import eu.domibus.ebms3.common.validators.ConfigurationValidator;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.property.PropertyResolver;
import eu.domibus.xml.XMLUtilImpl;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.jms.Topic;
import javax.persistence.EntityManagerFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * @author Cosmin Baciu
 * @since 3.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class PModeDaoTestIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PModeDaoTestIT.class);


    @org.springframework.context.annotation.Configuration
    static class ContextConfiguration {

        @Bean
        public ConfigurationDAO configurationDAO() {
            return Mockito.mock(ConfigurationDAO.class);
        }

        @Bean
        public ConfigurationRawDAO configurationRawDAO() {
            return Mockito.mock(ConfigurationRawDAO.class);
        }

        @Bean(name = "domibusJTA")
        public EntityManagerFactory getdomibusJTA() {
            return Mockito.mock(EntityManagerFactory.class);
        }

        @Bean
        public DomainService domainService() {
            return Mockito.mock(DomainServiceImpl.class);
        }

        @Bean
        public DomainContextProvider domainContextProvider() {
            return Mockito.mock(DomainContextProviderImpl.class);
        }

        @Bean
        public PropertyResolver propertyResolver() {
            return Mockito.mock(PropertyResolver.class);
        }

        @Bean
        public DomibusConfigurationService domibusConfigurationService() {
            return Mockito.mock(DefaultDomibusConfigurationService.class);
        }

        @Bean
        public DomainDao domainDao() {
            return Mockito.mock(DomainDaoImpl.class);
        }

        @Bean
        public DomibusPropertyProvider domibusPropertyProvider() {
            return Mockito.mock(DomibusPropertyProviderImpl.class);
        }

        @Bean(name = "domibusProperties")
        public Properties domibusProperties() {
            return Mockito.mock(Properties.class);
        }

        @Bean
        public EntityManagerFactory entityManagerFactory() {
            return Mockito.mock(EntityManagerFactory.class);
        }

        @Bean
        public ProcessDao processDao() {
            return Mockito.mock(ProcessDao.class);
        }

        @Bean
        public JMSManager jmsManager() {
            return Mockito.mock(JMSManager.class);
        }

        @Bean
        public Topic clusterCommandTopic() {
            return Mockito.mock(Topic.class);
        }

        @Bean
        public PartyDao partyDao() {
            return Mockito.mock(PartyDao.class);
        }

        @Bean
        public JAXBContext jaxbContextConfig() throws JAXBException {
            return JAXBContext.newInstance("eu.domibus.common.model.configuration");
        }

        @Bean
        @Qualifier("jmsTemplateCommand")
        public JmsOperations jmsOperations() throws JAXBException {
            return Mockito.mock(JmsOperations.class);
        }

        @Bean
        public ConfigurationValidator validator() {
            return configuration -> Collections.emptyList();
        }

        @Bean
        public XMLUtil xmlUtil() {
            return new XMLUtilImpl();
        }

        @Bean
        public PModeDao pModeDao() {
            return new PModeDao();
        }
    }

    @Autowired
    PModeDao pModeDao;

    @Autowired
    DomainContextProvider domainContextProvider;

    @Autowired
    XMLUtil xmlUtil;

    @Autowired
    ConfigurationDAO configurationDAO;

    @Autowired
    ConfigurationRawDAO configurationRawDAO;

    @Autowired
    JAXBContext jaxbContext;

    @Before
    public void resetMocks() {
        Mockito.reset(configurationDAO);
        Mockito.reset(configurationRawDAO);

        when(domainContextProvider.getCurrentDomain()).thenReturn(DomainService.DEFAULT_DOMAIN);
    }


    @Test
    public void testUpdatePModeWithPmodeContainingWhiteSpace() throws Exception {


        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-configuration-with-whitespaces.xml");
        byte[] pModeBytes = IOUtils.toByteArray(xmlStream);
        UnmarshallerResult unmarshallerResult = xmlUtil.unmarshal(true, jaxbContext, new ByteArrayInputStream(pModeBytes), null);

        List<String> updatePmodeMessage = pModeDao.updatePModes(pModeBytes, "description");
        assertNotNull(updatePmodeMessage);
        assertTrue(updatePmodeMessage.size() > 0);

        ArgumentCaptor<Configuration> parameter = ArgumentCaptor.forClass(Configuration.class);
        Mockito.verify(configurationDAO).updateConfiguration(parameter.capture());

        //compare the provided PMode with the one that gets saved
        Configuration saved = parameter.getValue();
        assertNotNull(saved);
        Configuration original = unmarshallerResult.getResult();
        assertEquals(saved.getMpcsXml().getMpc().size(), original.getMpcsXml().getMpc().size());
        assertEquals(saved.getBusinessProcesses(), original.getBusinessProcesses());

        ArgumentCaptor<ConfigurationRaw> rawConfig = ArgumentCaptor.forClass(ConfigurationRaw.class);
        Mockito.verify(configurationRawDAO).create(rawConfig.capture());

        final ConfigurationRaw raw = rawConfig.getValue();
        assertNotNull(raw.getConfigurationDate());
        assertEquals(raw.getXml(), pModeBytes);
        assertEquals(raw.getDescription(), "description");
    }


    @Test
    public void testUpdatePModeWithValidPmode() throws Exception {
        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-configuration-valid.xml");
        byte[] pModeBytes = IOUtils.toByteArray(xmlStream);
        UnmarshallerResult unmarshallerResult = xmlUtil.unmarshal(true, jaxbContext, new ByteArrayInputStream(pModeBytes), null);

        List<String> updatePmodeMessage = pModeDao.updatePModes(pModeBytes, "description");
        //there are no warnings
        assertTrue(updatePmodeMessage.isEmpty());

        ArgumentCaptor<Configuration> parameter = ArgumentCaptor.forClass(Configuration.class);
        Mockito.verify(configurationDAO).updateConfiguration(parameter.capture());

        //compare the provided PMode with the one that gets saved
        Configuration saved = parameter.getValue();
        assertNotNull(saved);
        Configuration original = unmarshallerResult.getResult();
        assertEquals(saved.getMpcsXml().getMpc().size(), original.getMpcsXml().getMpc().size());
        assertEquals(saved.getBusinessProcesses(), original.getBusinessProcesses());
    }

    @Test
    public void testUpdatePModeWithXsdNotCompliantPmode() throws Exception {
        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-configuration-xsd-not-compliant.xml");
        byte[] pModeBytes = IOUtils.toByteArray(xmlStream);

        try {
            pModeDao.updatePModes(pModeBytes, "description");
            fail("The Pmode is invalid so it should have thrown an exception");
        } catch (XmlProcessingException e) {
            LOG.info("Exception thrown as expected due to invalid PMode");
            assertNotNull(e.getErrors());
            assertTrue(e.getErrors().size() > 0);
        }

        Mockito.verify(configurationDAO, never()).updateConfiguration(Mockito.anyObject());
    }
}
