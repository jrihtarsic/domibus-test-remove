package eu.domibus.common.dao;

import eu.domibus.common.model.logging.RawEnvelopeDto;
import eu.domibus.common.model.logging.RawEnvelopeLog;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * @author idragusa
 * @since 3.2.5
 */
//@thom test this class
@Repository
public class RawEnvelopeLogDao extends BasicDao<RawEnvelopeLog> {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(RawEnvelopeLogDao.class);

    public static final String MESSAGE_ID = "MESSAGE_ID";

    public RawEnvelopeLogDao() {
        super(RawEnvelopeLog.class);
    }

    public RawEnvelopeDto findRawXmlByMessageId(final String messageId) {
        TypedQuery<RawEnvelopeDto> namedQuery = em.createNamedQuery("RawDto.findByMessageId", RawEnvelopeDto.class);
        namedQuery.setParameter("MESSAGE_ID", messageId);
        try {
            return namedQuery.getSingleResult();
        } catch (NoResultException nr) {
            LOG.warn("The message should have an associate raw xml saved in the database.");
            return null;
        }
    }

    /**
     * Delete all the raw entries related to a given UserMessage id.
     *
     * @param messageId the id of the message.
     */
    public void deleteUserMessageRawEnvelope(final String messageId) {
        Query query = em.createNamedQuery("Raw.deleteByMessageID");
        query.setParameter(MESSAGE_ID, messageId);
        query.executeUpdate();
    }

    public boolean messageExist(final String messageId){
        Query query = em.createNamedQuery("Raw.messageExit");
        query.setParameter(MESSAGE_ID, messageId);
        final List resultList = query.getResultList();
        return resultList.size()>0;
    }

}
