package eu.domibus.web.rest;

import eu.domibus.api.party.PartyService;
import eu.domibus.core.message.testservice.TestService;
import eu.domibus.ebms3.common.model.Ebms3Constants;
import eu.domibus.ebms3.common.model.MessageExchangePattern;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.web.rest.ro.TestServiceRequestRO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * @author Tiago Miguel
 * @since 4.0
 */

@RestController
@RequestMapping(value = "/rest/testservice")
@Validated
public class TestServiceResource {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TestServiceResource.class);

    @Autowired
    protected TestService testService;

    @Autowired
    private PartyService partyService;

    @RequestMapping(value = "sender", method = RequestMethod.GET)
    public String getSenderParty() {
        return partyService.getGatewayPartyIdentifier();
    }

    @RequestMapping(value = "parties", method = RequestMethod.GET)
    public List<String> getTestParties() {
        return partyService.findPushToPartyNamesByServiceAndAction(Ebms3Constants.TEST_SERVICE, Ebms3Constants.TEST_ACTION);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public String submitTest(@RequestBody @Valid TestServiceRequestRO testServiceRequestRO) throws IOException, MessagingProcessingException {
        return testService.submitTest(testServiceRequestRO.getSender(), testServiceRequestRO.getReceiver());
    }

    @RequestMapping(value = "dynamicdiscovery", method = RequestMethod.POST)
    @ResponseBody
    public String submitTestDynamicDiscovery(@RequestBody @Valid TestServiceRequestRO testServiceRequestRO) throws IOException, MessagingProcessingException {
        return testService.submitTestDynamicDiscovery(testServiceRequestRO.getSender(), testServiceRequestRO.getReceiver(), testServiceRequestRO.getReceiverType());
    }
}
