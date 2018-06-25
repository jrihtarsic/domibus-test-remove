package eu.domibus.core.alerts.model.persist;

import eu.domibus.core.alerts.model.EventType;
import eu.domibus.ebms3.common.model.AbstractBaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.*;

@Entity
@Table(name = "TB_EVENT")
public class Event extends AbstractBaseEntity {

    private final static Logger LOG = LoggerFactory.getLogger(Event.class);

    @Column(name = "EVENT_TYPE")
    @Enumerated(EnumType.STRING)
    @NotNull
    private EventType type;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "REPORTING_TIME")
    @NotNull
    private Date reportingTime;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "key")
    @MapKeyEnumerated
    private Map<String, EventPropertyValue> properties = new HashMap<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "TB_EVENT_ALERT",
            joinColumns = {@JoinColumn(name = "FK_EVENT")},
            inverseJoinColumns = {@JoinColumn(name = "FK_ALERT")}
    )
    private Set<Alert> alerts = new HashSet<>();

    public void addAlert(Alert alert) {
        alerts.add(alert);
    }

    public void addProperty(final String key,final  EventPropertyValue eventPropertyValue){
        eventPropertyValue.setKey(key);
        properties.put(key,eventPropertyValue);
        eventPropertyValue.setEvent(this);
    }

    public Map<String, EventPropertyValue> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Date getReportingTime() {
        return reportingTime;
    }

    public void setReportingTime(Date reportingTime) {
        this.reportingTime = reportingTime;
    }

    public Set<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(Set<Alert> alerts) {
        this.alerts = alerts;
    }

    @Override
    public String toString() {
        return "Event{" +
                "type='" + type + '\'' +
                ", reportingTime=" + reportingTime +
                ", properties=" + properties +
                ", alerts=" + alerts +
                '}';
    }
}
