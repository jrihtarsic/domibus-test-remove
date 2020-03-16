package eu.domibus.core.pmode;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.pmode.*;
import eu.domibus.api.pmode.domain.LegConfiguration;
import eu.domibus.api.pmode.domain.ReceptionAwareness;
import eu.domibus.common.MSHRole;
import eu.domibus.common.NotificationStatus;
import eu.domibus.core.message.MessagingDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.services.MessageExchangeService;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.pmode.validation.PModeValidationHelper;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.messaging.XmlProcessingException;
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

    @Autowired
    PModeValidationHelper pModeValidationHelper;

    @Override
    public LegConfiguration getLegConfiguration(String messageId) {
        final UserMessage userMessage = messagingDao.findUserMessageByMessageId(messageId);
        boolean isPull = messageExchangeService.forcePullOnMpc(userMessage);
        String pModeKey = null;
        try {
            pModeKey = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING, isPull).getPmodeKey();
        } catch (EbMS3Exception e) {
            throw new PModeException(DomibusCoreErrorCode.DOM_001, "Could not get the PMode key for message [" + messageId + "]. Pull [" + isPull + "]", e);
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
    public List<ValidationIssue> updatePModeFile(byte[] bytes, String description) throws PModeValidationException {
        try {
            return pModeProvider.updatePModes(bytes, description);
        } catch (XmlProcessingException e) {
            throw pModeValidationHelper.getPModeValidationException(e, "Failed to upload the PMode file due to: ");
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
