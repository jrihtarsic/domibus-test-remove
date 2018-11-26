package eu.domibus.xml;

import eu.domibus.api.util.xml.UnmarshallerResult;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import java.io.InputStream;

import static org.junit.Assert.*;


/**
 * Created by Cosmin Baciu on 14-Sep-16.
 */
@RunWith(JMockit.class)
public class XMLUtilImplTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(XMLUtilImplTest.class);

    @Tested
    XMLUtilImpl xmlUtil;

    @Test
    public void testUnmarshalWithNoWhiteSpaces() throws Exception {
        InputStream xsdStream = getClass().getClassLoader().getResourceAsStream(PModeProvider.SCHEMAS_DIR + PModeProvider.DOMIBUS_PMODE_XSD);
        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-configuration-valid.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance("eu.domibus.common.model.configuration");
        UnmarshallerResult unmarshal = xmlUtil.unmarshal(false, jaxbContext, xmlStream, xsdStream);
        assertNotNull(unmarshal.getResult());
        assertTrue(unmarshal.getResult() instanceof Configuration);
        assertTrue(unmarshal.getErrors() == null || unmarshal.getErrors().size() == 0);
        assertTrue(unmarshal.isValid());
    }

    @Test
    public void testUnmarshalSplittingWithNoWhiteSpaces() throws Exception {
        InputStream xsdStream = getClass().getClassLoader().getResourceAsStream(PModeProvider.SCHEMAS_DIR + PModeProvider.DOMIBUS_PMODE_XSD);
        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-configuration-splitting-valid.xml");
//        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-pmode.xsd.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance("eu.domibus.common.model.configuration");
        UnmarshallerResult unmarshal = xmlUtil.unmarshal(false, jaxbContext, xmlStream, xsdStream);
        assertNotNull(unmarshal.getResult());
        assertTrue(unmarshal.getResult() instanceof Configuration);
        LOG.info(StringUtils.join(unmarshal.getErrors(), ";"));
        assertTrue(unmarshal.getErrors() == null || unmarshal.getErrors().size() == 0);
        assertTrue(unmarshal.isValid());
    }

    @Test
    public void testUnmarshalWithWhiteSpaces() throws Exception {
        InputStream xsdStream = getClass().getClassLoader().getResourceAsStream(PModeProvider.SCHEMAS_DIR + PModeProvider.DOMIBUS_PMODE_XSD);
        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-configuration-with-whitespaces.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance("eu.domibus.common.model.configuration");
        UnmarshallerResult unmarshal = xmlUtil.unmarshal(false, jaxbContext, xmlStream, xsdStream);
        assertNotNull(unmarshal.getResult());
        assertTrue(unmarshal.getResult() instanceof Configuration);
        assertTrue(unmarshal.getErrors() != null && unmarshal.getErrors().size() > 0);
        LOG.debug("Validation errors: [" + unmarshal.getErrorMessage() + "]");
        assertFalse(unmarshal.isValid());
    }

    @Test
    public void testUnmarshalWithWhiteSpacesIgnored() throws Exception {
        InputStream xsdStream = getClass().getClassLoader().getResourceAsStream(PModeProvider.SCHEMAS_DIR + PModeProvider.DOMIBUS_PMODE_XSD);
        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-configuration-with-whitespaces.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance("eu.domibus.common.model.configuration");
        UnmarshallerResult unmarshal = xmlUtil.unmarshal(true, jaxbContext, xmlStream, xsdStream);
        assertNotNull(unmarshal.getResult());
        assertTrue(unmarshal.getResult() instanceof Configuration);
        assertTrue(unmarshal.getErrors() == null || unmarshal.getErrors().size() == 0);
        assertTrue(unmarshal.isValid());
    }
}
