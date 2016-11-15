package eu.domibus.common.validators;

import eu.domibus.common.exception.EbMS3Exception;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Created by venugar on 27/10/2016.
 */


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class EbMS3MessageValidatorTest {

    private static final Log LOG = LogFactory.getLog(EbMS3MessageValidatorTest.class);

    /*@org.springframework.context.annotation.Configuration
    static class ContextConfiguration {

        @Bean
        public ConfigurationDAO configurationDAO() {
            return Mockito.mock(ConfigurationDAO.class);
        }

        @Bean
        public EntityManagerFactory entityManagerFactory() {
            return Mockito.mock(EntityManagerFactory.class);
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
    XMLUtil xmlUtil;*/

    @org.springframework.context.annotation.Configuration
    static class ContextConfiguration {

        @Bean
        public BackendMessageValidator backendMessageValidatorObj() {
            return new BackendMessageValidator();
        }
    }


    @Autowired
    BackendMessageValidator backendMessageValidatorObj;



    @Test
    public void validateMessageIdFromSender() throws Exception {
        backendMessageValidatorObj.validateMessageId("null");
    }


    @Test
    public void validateMessageId() throws Exception {

        EbMS3MessageValidator ebMS3MessageValidator = new EbMS3MessageValidator();

        /*Happy Flow No error should occur*/
        try {
            String messageId1 = "1234567890-123456789-01234567890/1234567890/`~!@#$%^&*()-_=+\\|,<.>/?;:'\"|\\[{]}.567890.1234567890-1234567890?1234567890#1234567890!1234567890$1234567890%1234567890|12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012";
            ebMS3MessageValidator.validateMessageId(messageId1);

            String messageId1_1 = "40b0-9ffc-3f4cfa88bf8b@domibus.eu";
            ebMS3MessageValidator.validateMessageId(messageId1_1);

            String messageId1_2 = "APP-RESPONSE-d8d85972-64fb-4161-a1fb-996aa7a9c39c-DOCUMENT-BUNDLE";
            ebMS3MessageValidator.validateMessageId(messageId1_2);

            String messageId1_3 = "<1234>";
            ebMS3MessageValidator.validateMessageId(messageId1_3);

            String messageId1_4 = "^12^3$4";
            ebMS3MessageValidator.validateMessageId(messageId1_4);

        } catch (EbMS3Exception e1) {
            LOG.error(e1);
            Assert.fail();
        }
         /*Happy Flow No error should occur*/

        /*Message Id with leading and/or trailing whitespaces should throw error*/
        try {
            String messageId2 = "\t\t346ea37f-7583-40b0-9ffc-3f4cfa88bf8b@domibus.eu\t\t";
            ebMS3MessageValidator.validateMessageId(messageId2);
            Assert.fail();
        } catch (EbMS3Exception e2) {
            Assert.assertEquals("EBMS_0009", e2.getErrorCode());
        }
        /*Message Id with leading and/or trailing whitespaces should throw error*/


        /*Message Id containing non printable control characters should result in error*/
        try {
            String messageId4 = "346ea\b37f-7583-40\u0010b0-9ffc-3f4\u007Fcfa88bf8b@d\u0001omibus.eu";
            ebMS3MessageValidator.validateMessageId(messageId4);
            Assert.fail();
        } catch (EbMS3Exception e2) {
            Assert.assertEquals("EBMS_0009", e2.getErrorCode());
        }
        /*Message Id containing non printable control characters should result in error*/

        /*Message Id containing only non printable control characters should result in error*/
        try {
            String messageId5 = "\b\u0010\u0030\u007F\u0001";
            ebMS3MessageValidator.validateMessageId(messageId5);
            Assert.fail();
        } catch (EbMS3Exception e2) {
            Assert.assertEquals("EBMS_0009", e2.getErrorCode());
        }
        /*Message Id containing non printable control characters should result in error*/

        /*Message id more than 255 characters long should result in error*/
        try {
            String messageId6 = "1234567890-123456789-01234567890/1234567890/1234567890.1234567890.123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890@domibus.eu";
            ebMS3MessageValidator.validateMessageId(messageId6);
            Assert.fail();
        } catch (EbMS3Exception e2) {
            Assert.assertEquals("EBMS_0008", e2.getErrorCode());
        }
        /*Message id more than 255 characters long should result in error*/

        /*Message id should not be null*/
        try {
            String messageId8 = null;
            ebMS3MessageValidator.validateMessageId(messageId8);
            Assert.fail();
        } catch (EbMS3Exception e2) {
            Assert.assertEquals("EBMS_0009", e2.getErrorCode());
        }
        /*Message id should not be null*/

    }

   /*     @Test
    public void main()
    {
        String s= "APP-RESPONSE-d8d85972-64  ~!@#$%^&*())__++=-\\';fb-4161-a1fb-996aa7a9c39c-DOCUMENT-BUNDLE" ;
        System.out.println(s);
        Pattern patternControlChar = Pattern.compile("^[\\x20-\\x7E]*$");
        Matcher m = patternControlChar.matcher(s);

        System.out.println(m.matches());
    }*/

}