package eu.domibus.core.alerts.model.persist;

import eu.domibus.ebms3.common.model.AbstractBaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.krb5.internal.crypto.Aes128;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "TB_EVENT")
public class Event extends AbstractBaseEntity {

    private final static Logger LOG = LoggerFactory.getLogger(Event.class);

    @Column(name = "EVENT_TYPE")
    private String eventType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "REPORTING_TIME")
    private Date reportingTime;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "TB_EVENT_ALERT",
            joinColumns = {@JoinColumn(name = "EVENT_ID")},
            inverseJoinColumns = {@JoinColumn(name = "ALERT_ID")}
    )
    private Set<Alert> alerts = new HashSet<>();

    public void addAlert(Alert alert) {
        alerts.add(alert);
    }

}
