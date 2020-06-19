package eu.domibus.common.dao;

import eu.domibus.AbstractIT;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.model.configuration.Identifier;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.PartyIdType;
import eu.domibus.core.message.MessagingDao;
import eu.domibus.core.message.UserMessageLogEntityBuilder;
import eu.domibus.core.message.pull.MessagePullDto;
import eu.domibus.core.party.PartyDao;
import eu.domibus.ebms3.common.model.MessageInfo;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.test.util.PojoInstaciatorUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
public class MessagingDaoTestIT extends AbstractIT {

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private PartyDao partyDao;

    @PersistenceContext(unitName = "domibusJTA")
    private EntityManager entityManager;

    @Test
    @Transactional
    @Rollback
    public void findMessagingOnStatusReceiverAndMpc() throws Exception {

        List<Identifier> identifiers = new ArrayList<>();
        Identifier identifier= new Identifier();
        identifier.setPartyId("domibus-blue");
        PartyIdType partyIdType1 = new PartyIdType();
        partyIdType1.setName("partyIdTypeUrn1");
        identifier.setPartyIdType(partyIdType1);
        identifiers.add(identifier);
        Party party = PojoInstaciatorUtil.instanciate(Party.class, " [name:domibus-blue]");
        party.setIdentifiers(identifiers);
        Identifier next = party.getIdentifiers().iterator().next();
        next.setPartyId("RED_MSH");
        PartyIdType partyIdType = next.getPartyIdType();
        partyIdType.setValue("party_id_value");
        entityManager.persist(partyIdType);
        partyDao.create(party);
        Messaging firstMessage = PojoInstaciatorUtil.instanciate(Messaging.class, "userMessage[partyInfo[to[role:test,partyId{[value:RED_MSH]}]]]");
        MessageInfo messageInfo = firstMessage.getUserMessage().getMessageInfo();
        messageInfo.setRefToMessageId(null);
        messageInfo.setMessageId("123456");
        firstMessage.getUserMessage().setMpc("http://mpc");
        firstMessage.setId(null);

        Messaging secondMessage = PojoInstaciatorUtil.instanciate(Messaging.class, "userMessage[partyInfo[to[role:test,partyId{[value:testParty]}]]]");
        MessageInfo secondMessageInfo = secondMessage.getUserMessage().getMessageInfo();
        secondMessageInfo.setMessageId("789101212");
        secondMessageInfo.setRefToMessageId(null);
        secondMessage.setId(null);
        messagingDao.create(firstMessage);
        //@thom fix this late because their is a weird contraint exception here.

        UserMessageLogEntityBuilder umlBuilder = UserMessageLogEntityBuilder.create()
                .setMessageId(messageInfo.getMessageId())
                .setMessageStatus(MessageStatus.READY_TO_PULL)
                .setMshRole(MSHRole.SENDING);
        userMessageLogDao.create(umlBuilder.build());

        List<MessagePullDto> testParty = messagingDao.findMessagingOnStatusReceiverAndMpc("RED_MSH", MessageStatus.READY_TO_PULL, "http://mpc");
        assertEquals(1, testParty.size());
        assertEquals("123456", testParty.get(0).getMessageId());
    }

}