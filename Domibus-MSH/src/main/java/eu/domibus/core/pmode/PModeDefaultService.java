package eu.domibus.core.pmode;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.pmode.PModeArchiveInfo;
import eu.domibus.api.pmode.PModeException;
import eu.domibus.api.pmode.PModeService;
import eu.domibus.api.pmode.domain.LegConfiguration;
import eu.domibus.api.pmode.domain.ReceptionAwareness;
import eu.domibus.common.MSHRole;
import eu.domibus.common.NotificationStatus;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.services.MessageExchangeService;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.messaging.XmlProcessingException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@Service
public class PModeDefaultService implements PModeService {

    @Autowired
    MessagingDao messagingDao;

    @Autowired
    private PModeProvider pModeProvider;


    @Autowired
    private MessageExchangeService messageExchangeService;

    @Override
    public LegConfiguration getLegConfiguration(String messageId) {
        final UserMessage userMessage = messagingDao.findUserMessageByMessageId(messageId);
        boolean isPull = false;
        if(userMessage != null && userMessage.getMpc() != null && messageExchangeService.forcePullOnMpc(userMessage.getMpc())) {
            isPull = true;
        }
        String pModeKey = null;
        try {
            pModeKey = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING, isPull).getPmodeKey();
        } catch (EbMS3Exception e) {
            throw new PModeException(DomibusCoreErrorCode.DOM_001, "Could not get the PMode key for message [" + messageId + "]", e);
        }
        eu.domibus.common.model.configuration.LegConfiguration legConfigurationEntity = pModeProvider.getLegConfiguration(pModeKey);
        return convert(legConfigurationEntity);
    }

    @Override
    public byte[] getPModeFile(int id) {
        return pModeProvider.getPModeFile(id);
    }

    @Override
    public PModeArchiveInfo getCurrentPMode() {
        return pModeProvider.getCurrentPmode();
    }

    @Override
    public List<String> updatePModeFile(byte[] bytes, String description) throws PModeException {
        try {
            return pModeProvider.updatePModes(bytes, description);
        } catch (XmlProcessingException e) {
            String message = "Failed to upload the PMode file due to: " + ExceptionUtils.getRootCauseMessage(e);
            if (CollectionUtils.isNotEmpty(e.getErrors())) {
                message += ";" + StringUtils.join(e.getErrors(), ";");
            }
            throw new PModeException(DomibusCoreErrorCode.DOM_001, message);
        }
    }


    protected LegConfiguration convert(eu.domibus.common.model.configuration.LegConfiguration legConfigurationEntity) {
        if (legConfigurationEntity == null) {
            return null;
        }
        final LegConfiguration result = new LegConfiguration();
        result.setReceptionAwareness(convert(legConfigurationEntity.getReceptionAwareness()));
        return result;
    }

    protected ReceptionAwareness convert(eu.domibus.common.model.configuration.ReceptionAwareness receptionAwarenessEntity) {
        if (receptionAwarenessEntity == null) {
            return null;
        }
        ReceptionAwareness result = new ReceptionAwareness();
        result.setDuplicateDetection(receptionAwarenessEntity.getDuplicateDetection());
        result.setName(receptionAwarenessEntity.getName());
        result.setRetryCount(receptionAwarenessEntity.getRetryCount());
        result.setRetryTimeout(receptionAwarenessEntity.getRetryTimeout());
        return result;
    }

    public NotificationStatus getNotificationStatus(eu.domibus.common.model.configuration.LegConfiguration legConfiguration) {
        return legConfiguration.getErrorHandling().isBusinessErrorNotifyProducer() ? NotificationStatus.REQUIRED : NotificationStatus.NOT_REQUIRED;
    }
}
