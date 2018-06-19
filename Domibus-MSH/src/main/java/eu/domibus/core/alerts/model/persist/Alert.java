package eu.domibus.core.alerts.model.persist;

import eu.domibus.core.alerts.model.AlertType;
import eu.domibus.ebms3.common.model.AbstractBaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "TB_ALERT")
public class Alert extends AbstractBaseEntity{

    private final static Logger LOG = LoggerFactory.getLogger(Alert.class);

    @Column(name = "PROCESSED")
    private boolean processed;

    @Column(name = "PROCESSED_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date processedTime;

    @Column(name = "ALERT_TYPE")
    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @Column(name = "REPORTING_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reportingTime;

    @Column(name = "ATTEMPTS_NUMBER")
    private Integer attempts;

    @Column(name = "MAX_ATTEMPTS_NUMBER")
    private Integer maxAttempts;

    @Column(name = "REPORTING_TIME_FAILURE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reportingTimeFailure;

    @ManyToMany(mappedBy = "alerts")
    private Set<Event> events = new HashSet<>();

    public void addEvent(Event event){
        events.add(event);
        event.addAlert(this);
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public Date getProcessedTime() {
        return processedTime;
    }

    public void setProcessedTime(Date processedTime) {
        this.processedTime = processedTime;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public Date getReportingTime() {
        return reportingTime;
    }

    public void setReportingTime(Date reportingTime) {
        this.reportingTime = reportingTime;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Date getReportingTimeFailure() {
        return reportingTimeFailure;
    }

    public void setReportingTimeFailure(Date reportingTimeFailure) {
        this.reportingTimeFailure = reportingTimeFailure;
    }
}
