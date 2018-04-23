package eu.domibus.messaging.jms;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.RegexUtil;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Cosmin Baciu
 * @since 3.2
 */
@RunWith(JMockit.class)
public class JMSDestinationHelperImplTest {

    @Injectable
    RegexUtil regexUtil;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Tested
    JMSDestinationHelperImpl jmsDestinationHelper;

    @Test
    public void testIsInternalWithNoInternalExpressionDefined() throws Exception {
        String queueName = "myQueue";
        new Expectations() {{
            domibusPropertyProvider.getProperty(anyString);
            result = null;
        }};

        boolean internal = jmsDestinationHelper.isInternal(queueName);
        Assert.assertFalse(internal);
    }

    @Test
    public void testIsInternal() throws Exception {
        final String queueName = "myQueue";
        new Expectations() {{
            domibusPropertyProvider.getProperty(anyString);
            result = "myexpression";

            regexUtil.matches("myexpression", queueName);
            result = true;
        }};

        boolean internal = jmsDestinationHelper.isInternal(queueName);
        Assert.assertTrue(internal);

    }
}
