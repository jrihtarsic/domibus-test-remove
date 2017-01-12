package eu.domibus.logging;

import eu.domibus.logging.api.MessageCode;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@RunWith(JMockit.class)
public class DefaultMessageConverterTest {

    @Tested
    DefaultMessageConverter defaultMessageConverter;

    @Test
    public void testGetMessage() throws Exception {
        final MessageCode testMessageCode = new MessageCode() {
            @Override
            public String getCode() {
                return "myTestCode";
            }

            @Override
            public String getMessage() {
                return "test message {}";
            }
        };
        final String message = defaultMessageConverter.getMessage(DomibusLogger.BUSINESS_MARKER, testMessageCode, "param1");

        System.out.println(message);
    }
}
