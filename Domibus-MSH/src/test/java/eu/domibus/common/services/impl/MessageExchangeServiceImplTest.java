package eu.domibus.common.services.impl;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pmode.PModeException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.dao.ConfigurationDAO;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.*;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.common.model.logging.UserMessageLogEntity;
import eu.domibus.common.validators.ProcessValidator;
import eu.domibus.core.pull.PullMessageService;
import eu.domibus.ebms3.common.context.MessageExchangeConfiguration;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.ebms3.common.model.MessagePullDto;
import eu.domibus.ebms3.common.model.SignalMessage;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.ebms3.sender.EbMS3MessageBuilder;
import eu.domibus.test.util.PojoInstaciatorUtil;
import org.apache.commons.lang3.Validate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jms.Queue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static eu.domibus.common.services.impl.MessageExchangeServiceImpl.DOMIBUS_PULL_REQUEST_SEND_PER_JOB_CYCLE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageExchangeServiceImplTest {

    @Mock
    private PModeProvider pModeProvider;

    @Mock
    private ConfigurationDAO configurationDao;

    @Mock
    private JMSManager jmsManager;

    @Mock
    private EbMS3MessageBuilder messageBuilder;

    @Mock
    private MessagingDao messagingDao;

    @Mock
    private UserMessageLogDao userMessageLogDao;

    @Mock
    private PullMessageService pullMessageService;

    @Mock
    private DomibusPropertyProvider domibusPropertyProvider;

    @Mock
    private DomibusConfigurationService domibusConfigurationService;

    @Mock
    private DomainContextProvider domainProvider;

    @Spy
    private ProcessValidator processValidator;

    @InjectMocks
    private MessageExchangeServiceImpl messageExchangeService;

    private Process process;

    private Party correctParty;

    @Before
    public void init() {
        correctParty = new Party();
        correctParty.setName("party1");
        process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "legs{[name:leg1,defaultMpc[name:test1,qualifiedName:qn1]];[name:leg2,defaultMpc[name:test2,qualifiedName:qn2]]}", "responderParties{[name:responder]}", "initiatorParties{[name:initiator]}");

        Service service = new Service();
        service.setName("service1");
        findLegByName("leg1").setService(service);

        service = new Service();
        service.setName("service2");
        findLegByName("leg2").setService(service);

        when(pModeProvider.getGatewayParty()).thenReturn(correctParty);
        when(configurationDao.configurationExists()).thenReturn(true);
        //when(configurationDao.read()).thenReturn(configuration);
        List<Process> processes = Lists.newArrayList(process);
        when(pModeProvider.findPullProcessesByInitiator(correctParty)).thenReturn(processes);
    }

    private LegConfiguration findLegByName(final String name) {
        final Set<LegConfiguration> filter = Sets.filter(process.getLegs(), new Predicate<LegConfiguration>() {
            @Override
            public boolean apply(LegConfiguration legConfiguration) {
                return name.equals(legConfiguration.getName());
            }
        });
        Validate.isTrue(filter.size() == 1);
        return filter.iterator().next();
    }

    @Test
    public void testSuccessFullOneWayPullConfiguration() throws Exception {
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "mepBinding[name:pull]", "legs{[name:leg1,defaultMpc[name:test1,qualifiedName:qn1]];[name:leg2,defaultMpc[name:test2,qualifiedName:qn2]]}", "initiatorParties{[name:resp1]}");
        MessageStatus messageStatus = getMessageStatus(process);
        assertEquals(MessageStatus.READY_TO_PULL, messageStatus);
    }


    private MessageStatus getMessageStatus(Process process) throws EbMS3Exception {
        List<Process> processes = Lists.newArrayList();
        processes.add(process);
        MessageExchangeConfiguration messageExchangeConfiguration = new MessageExchangeConfiguration("agreementName", "senderParty", "receiverParty", "service", "action", "leg");
        when(pModeProvider.findPullProcessesByMessageContext(messageExchangeConfiguration)).thenReturn(processes);
        return messageExchangeService.getMessageStatus(messageExchangeConfiguration);
    }

    @Test(expected = PModeException.class)
    public void testIncorrectMultipleProcessFoundForConfiguration() throws EbMS3Exception {
        MessageExchangeConfiguration messageExchangeConfiguration = new MessageExchangeConfiguration("agreementName", "senderParty", "receiverParty", "service", "action", "leg");
        List<Process> processes = Lists.newArrayList();
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "mepBinding[name:pull]");
        processes.add(process);
        process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "mepBinding[name:push]");
        processes.add(process);
        when(pModeProvider.findPullProcessesByMessageContext(messageExchangeConfiguration)).thenReturn(processes);
        messageExchangeService.getMessageStatus(messageExchangeConfiguration);
    }

    @Test
    public void testInitiatePullRequest() throws Exception {
        when(pModeProvider.isConfigurationLoaded()).thenReturn(true);
        when(domainProvider.getCurrentDomain()).thenReturn(new Domain("default", "Default"));
        when(domibusPropertyProvider.getIntegerDomainProperty(DOMIBUS_PULL_REQUEST_SEND_PER_JOB_CYCLE)).thenReturn(10);
        when(domibusConfigurationService.isMultiTenantAware()).thenReturn(false);

        ArgumentCaptor<JmsMessage> mapArgumentCaptor = ArgumentCaptor.forClass(JmsMessage.class);
        messageExchangeService.initiatePullRequest();
        verify(pModeProvider, times(1)).getGatewayParty();
        verify(jmsManager, times(20)).sendMapMessageToQueue(mapArgumentCaptor.capture(), any(Queue.class));
        String pModeKeyResult = "party1" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
                "responder" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
                "service1" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
                "Mock" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
                "Mock" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR + "leg1";

        TestResult testResult = new TestResult("qn1", pModeKeyResult, "false");
        pModeKeyResult = "party1" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
                "responder" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
                "service2" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
                "Mock" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR +
                "Mock" + MessageExchangeConfiguration.PMODEKEY_SEPARATOR + "leg2";

        testResult.chain(new TestResult("qn2", pModeKeyResult, "false"));
        final List<JmsMessage> allValues = mapArgumentCaptor.getAllValues();
        for (JmsMessage allValue : allValues) {
            assertTrue(testResult.testSucced(allValue.getProperties()));
        }
    }

    @Test
    public void testInitiatePullRequestWithoutConfiguration() throws Exception {
        when(pModeProvider.isConfigurationLoaded()).thenReturn(false);
        messageExchangeService.initiatePullRequest();
        verify(pModeProvider, times(0)).findPullProcessesByInitiator(any(Party.class));
    }


    @Test
    public void testInvalidRequest() throws Exception {
        when(messageBuilder.buildSOAPMessage(any(SignalMessage.class), any(LegConfiguration.class))).thenThrow(
                new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0004, "An error occurred while processing your request. Please check the message header for more details.", null, null));
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "legs{[name:leg1,defaultMpc[name:test1,qualifiedName:qn1]];[name:leg2,defaultMpc[name:test2,qualifiedName:qn2]]}", "initiatorParties{[name:initiator]}");

        List<Process> processes = Lists.newArrayList(process);
        when(pModeProvider.findPullProcessesByInitiator(correctParty)).thenReturn(processes);
        when(pModeProvider.findPullProcessesByMessageContext(any(MessageExchangeConfiguration.class))).thenReturn(Lists.newArrayList(process));
        messageExchangeService.initiatePullRequest();
        verify(jmsManager, times(0)).sendMessageToQueue(any(JmsMessage.class), any(Queue.class));
    }

    @Test
    public void extractProcessOnMpc() throws Exception {
        List<Process> processes = Lists.newArrayList(PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "mepBinding[name:pull]", "legs{[name:leg1,defaultMpc[name:test1,qualifiedName:qn1]];[name:leg2,defaultMpc[name:test2,qualifiedName:qn2]]}", "initiatorParties{[name:resp1]}"));
        when(pModeProvider.findPullProcessByMpc(eq("qn1"))).thenReturn(processes);
        PullContext pullContext = messageExchangeService.extractProcessOnMpc("qn1");
        assertEquals("resp1", pullContext.getInitiator().getName());
        assertEquals("party1", pullContext.getResponder().getName());
        assertEquals("oneway", pullContext.getProcess().getMep().getName());
    }

    @Test(expected = PModeException.class)
    public void extractProcessMpcWithNoProcess() throws Exception {
        when(pModeProvider.findPullProcessByMpc(eq("qn1"))).thenReturn(new ArrayList<Process>());
        messageExchangeService.extractProcessOnMpc("qn1");
    }

    @Test(expected = PModeException.class)
    public void extractProcessMpcWithNoToManyProcess() throws Exception {
        when(pModeProvider.findPullProcessByMpc(eq("qn1"))).thenReturn(Lists.newArrayList(new Process(), new Process()));
        messageExchangeService.extractProcessOnMpc("qn1");
    }

    @Test
    public void testRetrieveReadyToPullUserMessageIdWithNoMessage() {
        String mpc = "mpc";
        Party party = Mockito.mock(Party.class);

        Set<Identifier> identifiers = new HashSet<>();
        Identifier identifier = new Identifier();
        identifier.setPartyId("party1");
        identifiers.add(identifier);

        when(party.getIdentifiers()).thenReturn(identifiers);

        when(messagingDao.findMessagingOnStatusReceiverAndMpc(eq("party1"), eq(MessageStatus.READY_TO_PULL), eq(mpc))).thenReturn(Lists.<MessagePullDto>newArrayList());

        final String messageId = messageExchangeService.retrieveReadyToPullUserMessageId(mpc, party);
        assertNull(messageId);

    }

    @Test
    public void testRetrieveReadyToPullUserMessageIdWithMessage() {
        String mpc = "mpc";
        Party party = Mockito.mock(Party.class);

        Set<Identifier> identifiers = new HashSet<>();
        Identifier identifier = new Identifier();
        identifier.setPartyId("party1");
        identifiers.add(identifier);

        when(party.getIdentifiers()).thenReturn(identifiers);

        final String testMessageId = "testMessageId";
        when(pullMessageService.getPullMessageId(eq("party1"), eq(mpc))).thenReturn(testMessageId);
        UserMessageLogEntity userMessageLog = new UserMessageLogEntity();
        userMessageLog.setMessageStatus(MessageStatus.READY_TO_PULL);
        when(userMessageLogDao.findByMessageId(testMessageId)).thenReturn(userMessageLog);

        final String messageId = messageExchangeService.retrieveReadyToPullUserMessageId(mpc, party);
        assertEquals(testMessageId, messageId);

    }

    @Test
    public void testRetrieveReadyToPullUserMessageIdWithMessageWithEmptyIdentifier() {
        String mpc = "mpc";
        Party party = Mockito.mock(Party.class);

        Set<Identifier> identifiers = new HashSet<>();
        when(party.getIdentifiers()).thenReturn(identifiers);

        assertNull(messageExchangeService.retrieveReadyToPullUserMessageId(mpc, party));

    }

    @Test
    public void testRetrieveReadyToPullUserMessageINoMessage() {
        String mpc = "mpc";
        Party party = Mockito.mock(Party.class);

        Set<Identifier> identifiers = new HashSet<>();
        Identifier identifier = new Identifier();
        identifier.setPartyId("party1");
        identifiers.add(identifier);

        when(party.getIdentifiers()).thenReturn(identifiers);

        when(pullMessageService.getPullMessageId(eq("party1"), eq(mpc))).thenReturn(null);
        assertNull(messageExchangeService.retrieveReadyToPullUserMessageId(mpc, party));

    }


    @Test
    public void testGetMessageStatusWhenNoPullProcessFound() {
        MessageExchangeConfiguration messageExchangeConfiguration = new MessageExchangeConfiguration("agr1",
                "sender",
                "receiver",
                "serv1",
                "action1",
                "leg1");
        when(pModeProvider.findPullProcessesByMessageContext(messageExchangeConfiguration)).thenReturn(Lists.<Process>newArrayList());
        final MessageStatus messageStatus = messageExchangeService.getMessageStatus(messageExchangeConfiguration);
        assertEquals(MessageStatus.SEND_ENQUEUED, messageStatus);

    }

    @Test
    public void testRetrieveMessageRestoreStatusWithValidPull() throws EbMS3Exception {
        MessageExchangeConfiguration messageExchangeConfiguration = new MessageExchangeConfiguration("agr1",
                "sender",
                "receiver",
                "serv1",
                "action1",
                "leg1");
        UserMessage userMessage = new UserMessage();
        userMessage.setMpc("mpc123");
        when(messagingDao.findUserMessageByMessageId("123")).thenReturn(userMessage);
        when(pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING)).thenReturn(messageExchangeConfiguration);
        when(pModeProvider.findPullProcessesByMessageContext(messageExchangeConfiguration)).thenReturn(Lists.newArrayList(process));
        final MessageStatus messageStatus = messageExchangeService.retrieveMessageRestoreStatus("123");
        assertEquals(MessageStatus.READY_TO_PULL, messageStatus);

    }


}