
package eu.domibus.common.services.impl;

import com.fasterxml.uuid.NoArgGenerator;
import eu.domibus.api.property.DomibusPropertyProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.DOMIBUS_MSH_MESSAGEID_SUFFIX;

/**
 * @author Christian Koch, Stefan Mueller
 */
public class MessageIdGenerator {
    private static final String MESSAGE_ID_SUFFIX_PROPERTY = DOMIBUS_MSH_MESSAGEID_SUFFIX;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected NoArgGenerator uuidGenerator;

    @Transactional(propagation = Propagation.SUPPORTS)
    public String generateMessageId() {
        String messageIdSuffix = domibusPropertyProvider.getProperty(MESSAGE_ID_SUFFIX_PROPERTY);
        return uuidGenerator.generate() + "@" + messageIdSuffix;
    }
}
