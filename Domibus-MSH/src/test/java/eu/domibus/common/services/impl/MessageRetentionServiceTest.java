package eu.domibus.common.services.impl;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.core.pmode.PModeProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 3.2.2
 */
@RunWith(JMockit.class)
public class MessageRetentionServiceTest {

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private PModeProvider pModeProvider;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private UserMessageService userMessageService;

    @Tested
    MessageRetentionService messageRetentionService;

    @Test
    public void testDeleteExpiredMessages() throws Exception {
        final String mpc1 = "mpc1";
        final String mpc2 = "mpc2";
        final List<String> mpcs = Arrays.asList(new String[]{mpc1, mpc2});

        new Expectations(messageRetentionService) {{
            pModeProvider.getMpcURIList();
            result = mpcs;

            messageRetentionService.getRetentionValue(MessageRetentionService.DOMIBUS_RETENTION_WORKER_MESSAGE_RETENTION_DOWNLOADED_MAX_DELETE);
            result = 10;

            messageRetentionService.getRetentionValue(MessageRetentionService.DOMIBUS_RETENTION_WORKER_MESSAGE_RETENTION_NOT_DOWNLOADED_MAX_DELETE);
            result = 20;
        }};

        messageRetentionService.deleteExpiredMessages();

        new Verifications() {{
            messageRetentionService.deleteExpiredMessages(mpc1, 10, 20);
        }};
    }

    @Test
    public void testDeleteExpiredMessagesForMpc() throws Exception {
        final String mpc1 = "mpc1";
        final Integer expiredDownloadedMessagesLimit = 10;
        final Integer expiredNotDownloadedMessagesLimit = 20;

        new Expectations(messageRetentionService) {{
            //partial mocking of the following methods
            messageRetentionService.deleteExpiredDownloadedMessages(mpc1, expiredDownloadedMessagesLimit);
            messageRetentionService.deleteExpiredNotDownloadedMessages(mpc1, expiredNotDownloadedMessagesLimit);
        }};

        messageRetentionService.deleteExpiredMessages(mpc1, 10, 20);

        //the verifications are done in the Expectations block

    }

    @Test
    public void testDeleteExpiredDownloadedMessagesWithNegativeRetentionValue() throws Exception {
        final String mpc1 = "mpc1";

        new Expectations(messageRetentionService) {{
            pModeProvider.getRetentionDownloadedByMpcURI(mpc1);
            result = -1;
        }};

        messageRetentionService.deleteExpiredDownloadedMessages(mpc1, 10);

        new Verifications() {{
            userMessageLogDao.getDownloadedUserMessagesOlderThan(withAny(new Date()), anyString, null);
            times = 0;
        }};
    }

    @Test
    public void testDeleteExpiredNotDownloadedMessagesWithNegativeRetentionValue() throws Exception {
        final String mpc1 = "mpc1";

        new Expectations(messageRetentionService) {{
            pModeProvider.getRetentionUndownloadedByMpcURI(mpc1);
            result = -1;
        }};

        messageRetentionService.deleteExpiredNotDownloadedMessages(mpc1, 10);

        new Verifications() {{
            userMessageLogDao.getUndownloadedUserMessagesOlderThan(withAny(new Date()), anyString, null);
            times = 0;
        }};
    }

    @Test
    public void testDeleteExpiredDownloadedMessages() throws Exception {
        String id1 = "1";
        String id2 = "2";
        final List<String> downloadedMessageIds = Arrays.asList(new String[]{id1, id2});
        final String mpc1 = "mpc1";
        final Integer messagesDeleteLimit = 5;

        new Expectations(messageRetentionService) {{
            pModeProvider.getRetentionDownloadedByMpcURI(mpc1);
            result = 10;

            userMessageLogDao.getDownloadedUserMessagesOlderThan(withAny(new Date()), mpc1, null);
            result = downloadedMessageIds;
        }};

        messageRetentionService.deleteExpiredDownloadedMessages(mpc1, messagesDeleteLimit);

        new Verifications() {{
            userMessageService.delete(downloadedMessageIds);
        }};
    }

    @Test
    public void testDeleteExpiredNotDownloadedMessages() throws Exception {
        String id1 = "1";
        String id2 = "2";
        final List<String> downloadedMessageIds = Arrays.asList(new String[]{id1, id2});
        final String mpc1 = "mpc1";
        final Integer messagesDeleteLimit = 5;

        new Expectations(messageRetentionService) {{
            pModeProvider.getRetentionUndownloadedByMpcURI(mpc1);
            result = 10;

            userMessageLogDao.getUndownloadedUserMessagesOlderThan(withAny(new Date()), mpc1, null);
            result = downloadedMessageIds;
        }};

        messageRetentionService.deleteExpiredNotDownloadedMessages(mpc1, messagesDeleteLimit);

        new Verifications() {{
            userMessageService.delete(downloadedMessageIds);
        }};
    }

    @Test(expected = NumberFormatException.class)
    public void testGetRetentionValueWithUndefinedRetentionValue() throws Exception {
        final String propertyName = "retentionLimitProperty";

        new Expectations(messageRetentionService) {{
            domibusPropertyProvider.getDomainProperty(propertyName);
            result = null;
        }};

        messageRetentionService.getRetentionValue(propertyName);
    }

    @Test(expected = NumberFormatException.class)
    public void testGetRetentionValueWithInvalidRetentionValue() throws Exception {
        final String propertyName = "retentionLimitProperty";

        new Expectations(messageRetentionService) {{
            domibusPropertyProvider.getDomainProperty(propertyName);
            result = "a2";
        }};

        messageRetentionService.getRetentionValue(propertyName);
    }

    @Test
    public void testGetRetentionValueWithValidRetentionValue() throws Exception {
        final String propertyName = "retentionLimitProperty";

        new Expectations(messageRetentionService) {{
            domibusPropertyProvider.getDomainProperty(propertyName);
            result = "5";
        }};

        final Integer retentionValue = messageRetentionService.getRetentionValue(propertyName);
        Assert.assertEquals(retentionValue, Integer.valueOf(5));
    }
}
