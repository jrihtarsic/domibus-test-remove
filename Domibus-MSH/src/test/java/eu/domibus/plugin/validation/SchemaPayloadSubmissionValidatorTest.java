package eu.domibus.plugin.validation;

import eu.domibus.plugin.Submission;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.Resource;

import javax.xml.bind.JAXBContext;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by baciuco on 08/08/2016.
 */
@RunWith(JMockit.class)
public class SchemaPayloadSubmissionValidatorTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SchemaPayloadSubmissionValidator.class);

    @Injectable
    JAXBContext jaxbContext;

    @Injectable
    Resource schema;

    @Tested
    SchemaPayloadSubmissionValidator schemaPayloadSubmissionValidator;

    @Test
    public void testValidateWithNoPayloads(@Injectable final Submission submission) throws Exception {
        new Expectations() {{
            submission.getPayloads();
            result = null;
        }};
        schemaPayloadSubmissionValidator.validate(submission);

        new Verifications() {{
            schema.getInputStream();
            times = 0;
        }};
    }

    @Test(expected = SubmissionValidationException.class)
    public void testValidateWithFirstPayloadInvalid(@Injectable final Submission submission,
                                                    @Injectable final Submission.Payload payload1,
                                                    @Injectable final Submission.Payload payload2) throws Exception {
        new Expectations() {{
            submission.getPayloads();
            Set<Submission.Payload> payloads = new HashSet<>();
            payloads.add(payload1);
            payloads.add(payload2);
            result = payloads;

            schema.getInputStream();
            result = null;
        }};
        schemaPayloadSubmissionValidator.validate(submission);

        new Verifications() {{
            payload2.getPayloadDatahandler().getInputStream();
            times = 0;
        }};
    }

    @Test
    public void testValidateWhenExceptionIsThrown(@Injectable final Submission submission,
                                                    @Injectable final Submission.Payload payload1) throws Exception {
        new Expectations() {{
            submission.getPayloads();
            Set<Submission.Payload> payloads = new HashSet<>();
            payloads.add(payload1);
            result = payloads;

            schema.getInputStream();
            result = new NullPointerException();
        }};
        try {
            schemaPayloadSubmissionValidator.validate(submission);
            Assert.fail("It should throw " + SubmissionValidationException.class.getCanonicalName());
        } catch (SubmissionValidationException e) {
            LOG.debug("SubmissionValidationException catched " + e.getMessage());
        }
        new Expectations() {{
            schema.getInputStream();
            result = new SubmissionValidationException();
        }};
        try {
            schemaPayloadSubmissionValidator.validate(submission);
            Assert.fail("It should throw " + SubmissionValidationException.class.getCanonicalName());
        } catch (SubmissionValidationException e) {
            LOG.debug("SubmissionValidationException catched " + e.getMessage());
        }

    }
}
