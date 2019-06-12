package eu.domibus.core.payload.persistence;

import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import eu.domibus.ebms3.common.model.PartInfo;
import eu.domibus.ebms3.common.model.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */
@Service
public class PayloadPersistenceProviderImpl implements PayloadPersistenceProvider {

    @Autowired
    protected PayloadFileStorageProvider storageProvider;

    @Autowired
    protected FileSystemPayloadPersistence fileSystemPayloadPersistence;

    @Autowired
    protected DatabasePayloadPersistence databasePayloadPersistence;

    @Override
    public PayloadPersistence getPayloadPersistence(PartInfo partInfo, UserMessage userMessage) {
        PayloadPersistence result = databasePayloadPersistence;
        if (!storageProvider.isPayloadsPersistenceInDatabaseConfigured()) {
            result = fileSystemPayloadPersistence;
        }
        return result;
    }
}
