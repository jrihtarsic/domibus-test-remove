package eu.domibus.core.alerts;

import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.dao.AlertDao;
import eu.domibus.core.alerts.dao.EventDao;
import eu.domibus.core.alerts.model.AlertType;
import eu.domibus.core.alerts.model.DefaultMailModel;
import eu.domibus.core.alerts.model.MailModel;
import eu.domibus.core.alerts.model.persist.Alert;
import eu.domibus.core.alerts.model.persist.AlertStatus;
import eu.domibus.core.alerts.model.persist.Event;
import eu.domibus.core.converter.DomainCoreConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.Queue;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static eu.domibus.core.alerts.model.persist.AlertStatus.RETRY;
import static eu.domibus.core.alerts.model.persist.AlertStatus.SENT;

@Service
public class AlertServiceImpl implements AlertService {

    private final static Logger LOG = LoggerFactory.getLogger(AlertServiceImpl.class);
    public static final String DOMIBUS_ALERT_RETRY_MAX_ATTEMPTS = "domibus.alert.retry.max_attempts";

    @Autowired
    private EventDao eventDao;

    @Autowired
    private AlertDao alertDao;

    @Autowired
    private DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    private DomainCoreConverter domainConverter;

    @Autowired
    private JMSManager jmsManager;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    @Qualifier("alertMessageQueue")
    private Queue alertMessageQueue;


    @Override
    @Transactional
    public eu.domibus.core.alerts.model.Alert createAlertOnEvent(eu.domibus.core.alerts.model.Event event) {
        final Event eventEntity = eventDao.read(event.getEntityId());
        Alert alert = new Alert();
        alert.addEvent(eventEntity);
        alert.setAlertType(AlertType.getAlertTypeFromEventType(event.getType()));
        alert.setAttempts(0);
        alert.setMaxAttempts(Integer.valueOf(domibusPropertyProvider.getProperty(DOMIBUS_ALERT_RETRY_MAX_ATTEMPTS, "1")));
        alert.setReportingTime(event.getReportingTime());
        alertDao.create(alert);
        final eu.domibus.core.alerts.model.Alert convert = domainConverter.convert(alert, eu.domibus.core.alerts.model.Alert.class);
        return convert;
    }

    @Override
    public void enqueueAlert(eu.domibus.core.alerts.model.Alert alert) {
        jmsManager.convertAndSendToQueue(alert, alertMessageQueue, "alert");
    }

    @Override
    public MailModel getMailModelForAlert(eu.domibus.core.alerts.model.Alert alert) {
        final Alert read = alertDao.read(alert.getEntityId());
        Map<String, String> mailModel = new HashMap<>();
        final Event next = read.getEvents().iterator().next();
        next.getProperties().forEach((key, value) -> mailModel.put(key, value.getValue()));
        final AlertType alertType = read.getAlertType();
        switch (alertType) {
            case MSG_COMMUNICATION_FAILURE:
                return new DefaultMailModel<Map>(mailModel, "/templates/message.html", "Message status change");
            default:
                LOG.error("Alert type[{}] is not supported for mail sending.", alertType);
                throw new IllegalArgumentException("Unsuported alert type.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSendAlertStatus(eu.domibus.core.alerts.model.Alert alert) {
        final Alert alertEntity = alertDao.read(alert.getEntityId());
        alertEntity.setAlertStatus(alert.getAlertStatus());
        alertEntity.setNextAttempt(null);
        if (SENT == alertEntity.getAlertStatus()) {
            alertEntity.setReportingTime(new Date());
            alertDao.update(alertEntity);
            return;
        }
        final Integer attempts = alertEntity.getAttempts();
        final Integer maxAttempts = alertEntity.getMaxAttempts();
        if (attempts < maxAttempts) {
            final Integer minutesBetweenAttempt = Integer.valueOf(domibusPropertyProvider.getProperty("domibus.alert.retry.time"));
            final Date nextAttempt = Date.from(LocalDateTime.now().plusMinutes(minutesBetweenAttempt).atZone(ZoneId.systemDefault()).toInstant());
            alertEntity.setNextAttempt(nextAttempt);
            alertEntity.setAttempts(attempts + 1);
            alertEntity.setAlertStatus(RETRY);
        }
        alertEntity.setReportingTimeFailure(new Date());
        alertDao.update(alertEntity);
    }

}
