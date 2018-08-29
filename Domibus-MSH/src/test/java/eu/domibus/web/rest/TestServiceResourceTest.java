package eu.domibus.web.rest;

import eu.domibus.api.party.PartyService;
import eu.domibus.core.message.test.TestService;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.ebms3.common.model.Ebms3Constants;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.handler.DatabaseMessageHandler;
import eu.domibus.web.rest.ro.TestServiceRequestRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Tiago Miguel
 * @since 4.0
 */
@RunWith(JMockit.class)
public class TestServiceResourceTest {

    @Tested
    TestServiceResource testServiceResource;

    @Injectable
    TestService testService;

    @Injectable
    PartyService partyService;

    @Injectable
    DatabaseMessageHandler databaseMessageHandler;

    @Injectable
    PModeProvider pModeProvider;

    @Test
    public void testGetSenderParty() {
        // Given
        new Expectations() {{
            partyService.getGatewayPartyIdentifier();
            result = "test";
        }};

        // When
        String senderParty = testServiceResource.getSenderParty();

        // Then
        Assert.assertEquals("test", senderParty);
    }

    @Test
    public void testGetTestParties() {
        // Given
        List<String> testPartiesList = new ArrayList<>();
        testPartiesList.add("testParty1");
        testPartiesList.add("testParty2");

        new Expectations() {{
            partyService.findPartyNamesByServiceAndAction(Ebms3Constants.TEST_SERVICE, Ebms3Constants.TEST_ACTION);
            result = testPartiesList;
        }};

        // When
        List<String> testParties = testServiceResource.getTestParties();

        // Then
        Assert.assertEquals(testPartiesList, testParties);
    }

    @Test
    public void testSubmitTest() throws IOException, MessagingProcessingException {
        // Given
        TestServiceRequestRO testServiceRequestRO = new TestServiceRequestRO();
        testServiceRequestRO.setSender("sender");
        testServiceRequestRO.setReceiver("receiver");

        new Expectations() {{
            testService.submitTest(testServiceRequestRO.getSender(),  testServiceRequestRO.getReceiver());
            result = "test";
        }};

        // When
        String submitTest = testServiceResource.submitTest(testServiceRequestRO);

        // Then
        Assert.assertEquals("test" ,submitTest);
    }

    @Test
    public void testSubmitTestDynamicDiscoveryMessage() throws IOException, MessagingProcessingException {
        // Given
        TestServiceRequestRO testServiceRequestRO = new TestServiceRequestRO();
        testServiceRequestRO.setSender("sender");
        testServiceRequestRO.setReceiver("receiver");
        testServiceRequestRO.setReceiverType("receiverType");
        testServiceRequestRO.setServiceType("servicetype");

        new Expectations() {{
            testService.submitTestDynamicDiscovery(testServiceRequestRO.getSender(),  testServiceRequestRO.getReceiver(), testServiceRequestRO.getReceiverType());
            result = "dynamicdiscovery";
        }};

        // When
        String submitTestDynamicDiscovery = testServiceResource.submitTestDynamicDiscovery(testServiceRequestRO);

        // Then
        Assert.assertEquals("dynamicdiscovery" ,submitTestDynamicDiscovery);
    }
}
