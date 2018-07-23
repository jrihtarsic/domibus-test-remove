package eu.domibus.core.replication;

import eu.domibus.common.dao.BasicDao;
import eu.domibus.ebms3.common.model.MessageSubtype;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Catalin Enache
 * @since 4.0
 */
@Repository
public class UIMessageDaoImpl extends BasicDao<UIMessageEntity> implements UIMessageDao {

    /**
     * logger
     */
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UIMessageDaoImpl.class);


    public UIMessageDaoImpl() {
        super(UIMessageEntity.class);
    }

    /**
     * find {@link UIMessageEntity} by messageId
     *
     * @param messageId
     * @return an instance of {@link UIMessageEntity}
     */
    @Override
    public UIMessageEntity findUIMessageByMessageId(final String messageId) {

        final TypedQuery<UIMessageEntity> query = this.em.createNamedQuery("UIMessageEntity.findUIMessageByMessageId", UIMessageEntity.class);
        query.setParameter(MESSAGE_ID, messageId);

        return DataAccessUtils.singleResult(query.getResultList());
    }

    /**
     * Counts the messages from {@code TB_MESSAGE_UI} table
     * filter object should contain messageType
     *
     * @param filters it should include messageType always - User or Signal message
     * @return number of messages
     */
    @Override
    public int countMessages(Map<String, Object> filters) {
        long startTime = System.currentTimeMillis();
        CriteriaBuilder cb = this.em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<UIMessageEntity> uiMessageEntity = cq.from(UIMessageEntity.class);
        cq.select(cb.count(uiMessageEntity));
        List<Predicate> predicates = getPredicates(filters, cb, uiMessageEntity);
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Long> query = em.createQuery(cq);

        Long result = query.getSingleResult();
        if (LOG.isDebugEnabled()) {
            final long endTime = System.currentTimeMillis();
            LOG.debug("[{}] milliseconds to countMessages", endTime - startTime);
        }
        return result.intValue();
    }

    /**
     * list the messages from {@code TB_MESSAGE_UI} table with pagination
     *
     * @param from    the beginning of the page
     * @param max     how many messages in a page
     * @param column  which column to order by
     * @param asc     ordering type - ascending or descending
     * @param filters it should include messageType always - User or Signal message
     * @return a list of {@link UIMessageEntity}
     */
    @Override
    public List<UIMessageEntity> findPaged(int from, int max, String column, boolean asc, Map<String, Object> filters) {
        long startTime = System.currentTimeMillis();
        List<UIMessageEntity> result;
        CriteriaBuilder cb = this.em.getCriteriaBuilder();
        CriteriaQuery<UIMessageEntity> cq = cb.createQuery(UIMessageEntity.class);
        Root<UIMessageEntity> ume = cq.from(UIMessageEntity.class);
        cq.select(ume);
        List<Predicate> predicates = getPredicates(filters, cb, ume);
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        if (column != null) {
            if (asc) {
                cq.orderBy(cb.asc(ume.get(column)));
            } else {
                cq.orderBy(cb.desc(ume.get(column)));
            }

        }
        TypedQuery<UIMessageEntity> query = this.em.createQuery(cq);
        query.setFirstResult(from);
        query.setMaxResults(max);

        result = query.getResultList();
        if (LOG.isDebugEnabled()) {
            final long endTime = System.currentTimeMillis();
            LOG.debug("[{}] milliseconds to findPaged [{}] messages", endTime - startTime, max);
        }
        return result;
    }

    /**
     * Creates or updates an existing {@link UIMessageEntity}
     *
     * @param uiMessageEntity
     */
    @Override
    public void saveOrUpdate(final UIMessageEntity uiMessageEntity) {
        UIMessageEntity uiMessageEntityFound = findUIMessageByMessageId(uiMessageEntity.getMessageId());
        if (uiMessageEntityFound != null) {
            uiMessageEntity.setEntityId(uiMessageEntityFound.getEntityId());
            uiMessageEntity.setVersion(uiMessageEntityFound.getVersion());
            em.merge(uiMessageEntity);
            LOG.info("uiMessageEntity having messageId={} have been updated", uiMessageEntity.getMessageId());
            return;
        }
        em.persist(uiMessageEntity);
        LOG.info("uiMessageEntity having messageId={} have been inserted", uiMessageEntity.getMessageId());
    }

    @Override
    @Transactional(readOnly = true, timeout = 300)
    public List<UIMessageDiffEntity> findUIMessagesNotSynced() {
        long startTime = System.currentTimeMillis();

        Query q = em.createQuery("Select * from U", UIMessageDiffEntity.class);
        List<UIMessageDiffEntity> result = q.getResultList();

        LOG.debug("[{}] milliseconds to findUIMessagesNotSynced", System.currentTimeMillis() - startTime);

        return result;
    }

    @Override
    @Transactional(readOnly = true, timeout = 300)
    public int countUIMessageDiffRows() {
        final Query nativeQuery = em.createNativeQuery("SELECT count(mud.MESSAGE_ID) FROM  V_MESSAGE_UI_DIFF mud");
        final Number singleResult = (Number) nativeQuery.getSingleResult();
        return singleResult.intValue();
    }

    /**
     * builds the predicates list based on search criteria (filters)
     *
     * @param filters
     * @param cb
     * @param ume
     * @return
     */
    protected List<Predicate> getPredicates(Map<String, Object> filters, CriteriaBuilder cb, Root<? extends UIMessageEntity> ume) {
        List<Predicate> predicates = new ArrayList<>();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            String filterKey = filter.getKey();
            Object filterValue = filter.getValue();
            if (filterValue != null) {
                if (filterValue instanceof String) {
                    if (StringUtils.isNotBlank(filterKey) && !filter.getValue().toString().isEmpty()) {
                        switch (filterKey) {
                            case "fromPartyId":
                                predicates.add(cb.equal(ume.get("fromId"), filterValue));
                                break;
                            case "toPartyId":
                                predicates.add(cb.equal(ume.get("toId"), filterValue));
                                break;
                            case "originalSender":
                                predicates.add(cb.equal(ume.get("fromScheme"), filterValue));
                                break;
                            case "finalRecipient":
                                predicates.add(cb.equal(ume.get("toScheme"), filterValue));
                                break;
                            default:
                                predicates.add(cb.equal(ume.get(filterKey), filterValue));
                                break;
                        }
                    }
                } else if (filterValue instanceof Date) {
                    if (!filterValue.toString().isEmpty()) {
                        switch (filterKey) {
                            case "receivedFrom":
                                predicates.add(cb.greaterThanOrEqualTo(ume.<Date>get("received"), Timestamp.valueOf(filterValue.toString())));
                                break;
                            case "receivedTo":
                                predicates.add(cb.lessThanOrEqualTo(ume.<Date>get("received"), Timestamp.valueOf(filterValue.toString())));
                                break;
                            default:
                                break;
                        }
                    }
                } else {
                    predicates.add(cb.equal(ume.<String>get(filterKey), filterValue));
                }
            } else {
                if (filterKey.equals("messageSubtype")) {
                    predicates.add(cb.isNull(ume.<MessageSubtype>get("messageSubtype")));
                }
            }
        }
        return predicates;
    }


}