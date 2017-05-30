package eu.domibus.common.model.configuration;

import eu.domibus.ebms3.common.model.AbstractBaseEntity;

import javax.persistence.*;
import javax.xml.bind.annotation.*;
import java.util.HashSet;
import java.util.Set;

import static eu.domibus.common.model.configuration.Process.*;

/**
 * @author Christian Koch, Stefan Mueller
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "initiatorPartiesXml",
        "responderPartiesXml",
        "legsXml"
})
@Entity
@Table(name = "TB_PROCESS")
@NamedQueries({
        @NamedQuery(name = RETRIEVE_FROM_MESSAGE_CONTEXT, query = "SELECT p FROM Process as p left join p.agreement as a left join p.legs as l left join p.initiatorParties init left join p.responderParties resp  where l.action.name=:action and l.service.name=:service and (a is null  or a.name=:agreement) and l.name=:leg and init.name=:initiatorName and resp.name=:responderName"),
        //@thom this is the place to swith initiator and responder.
        @NamedQuery(name = FIND_PULL_PROCESS_TO_INITIATE, query = "SELECT p FROM Process as p join p.initiatorParties as ini WHERE p.mepBinding.name=:mepBinding and ini in(:initiator)"),
        @NamedQuery(name = FIND_PULL_PROCESS_TO_ANSWER, query = "SELECT p FROM Process as p left join p.legs as l left join p.initiatorParties init where p.mepBinding.name=:mepBinding and l.defaultMpc.qualifiedName=:mpcName and init.name=:initiator")})
public class Process extends AbstractBaseEntity {
    @Transient
    @XmlTransient
    public final static String RETRIEVE_FROM_MESSAGE_CONTEXT = "Process.retrieveFromMessageContext";
    public final static String FIND_PULL_PROCESS_TO_INITIATE = "Process.findPullProcessToInitiate";
    public final static String FIND_PULL_PROCESS_TO_ANSWER = "Process.findPullProcessToAnswer";
    @XmlAttribute(name = "name", required = true)
    @Column(name = "NAME")
    protected String name;

    @XmlElement(required = true, name = "initiatorParties")
    @Transient
    protected InitiatorParties initiatorPartiesXml;
    @XmlElement(required = true, name = "responderParties")
    @Transient
    protected ResponderParties responderPartiesXml;
    @XmlElement(required = true, name = "legs")
    @Transient
    protected Legs legsXml;
    @XmlAttribute(name = "initiatorRole", required = true)
    @Transient
    protected String initiatorRoleXml;
    @Transient
    @XmlAttribute(name = "responderRole", required = true)
    protected String responderRoleXml;
    @XmlAttribute(name = "agreement", required = true)
    @Transient
    protected String agreementXml;
    @XmlAttribute(name = "mep", required = true)
    @Transient
    protected String mepXml;
    @XmlAttribute(name = "binding", required = true)
    @Transient
    protected String bindingXml;
    @XmlTransient
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "TB_JOIN_PROCESS_INIT_PARTY", joinColumns = @JoinColumn(name = "PROCESS_FK"), inverseJoinColumns = @JoinColumn(name = "PARTY_FK"))
    private Set<Party> initiatorParties;
    @XmlTransient
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "TB_JOIN_PROCESS_RESP_PARTY", joinColumns = @JoinColumn(name = "PROCESS_FK"), inverseJoinColumns = @JoinColumn(name = "PARTY_FK"))
    private Set<Party> responderParties;

    @XmlTransient
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "TB_JOIN_PROCESS_LEG", joinColumns = @JoinColumn(name = "PROCESS_FK"), inverseJoinColumns = @JoinColumn(name = "LEG_FK"))
    private Set<LegConfiguration> legs;
    @XmlTransient
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK_INITIATOR_ROLE")
    private Role initiatorRole;
    @XmlTransient
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK_RESPONDER_ROLE")
    private Role responderRole;
    @XmlTransient
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK_AGREEMENT")
    private Agreement agreement;
    @XmlTransient
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK_MEP")
    private Mep mep;
    @XmlTransient
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK_MEP_BINDING")
    private Binding mepBinding;
    @XmlTransient
    @Column(name = "USE_DYNAMIC_RESPONDER")
    private boolean dynamicResponder;
    @XmlTransient
    @Column(name = "USE_DYNAMIC_INITIATOR")
    private boolean dynamicInitiator;


    public void init(final Configuration configuration) {
        this.initiatorParties = new HashSet<>();
        if (initiatorPartiesXml != null) { // empty means dynamic discovery is used
            for (final InitiatorParty ini : this.initiatorPartiesXml.getInitiatorParty()) {
                for (final Party party : configuration.getBusinessProcesses().getParties()) {
                    if (party.getName().equals(ini.getName())) {
                        this.initiatorParties.add(party);
                        break;
                    }
                }
            }
        } else {
            this.dynamicInitiator = true;
        }

        this.responderParties = new HashSet<>();
        if (responderPartiesXml != null) { // empty means dynamic discovery is used
            for (final ResponderParty res : this.responderPartiesXml.getResponderParty()) {
                for (final Party party : configuration.getBusinessProcesses().getParties()) {
                    if (party.getName().equals(res.getName())) {
                        this.responderParties.add(party);
                        break;
                    }
                }
            }
        } else {
            this.dynamicResponder = true;
        }

        this.legs = new HashSet<>();
        for (final Leg leg : this.legsXml.getLeg()) {
            for (final LegConfiguration legConfiguration : configuration.getBusinessProcesses().getLegConfigurations()) {
                if (legConfiguration.getName().equals(leg.getName())) {
                    this.legs.add(legConfiguration);
                    break;
                }
            }
        }

        for (final Role role : configuration.getBusinessProcesses().getRoles()) {
            if (role.getName().equals(this.initiatorRoleXml)) {
                this.initiatorRole = role;
            }
            if (role.getName().equals(this.responderRoleXml)) {
                this.responderRole = role;
            }
        }

        for (final Agreement agreement1 : configuration.getBusinessProcesses().getAgreements()) {
            if (agreement1.getName().equals(this.agreementXml)) {
                this.agreement = agreement1;
                break;
            }
        }
        for (final Mep mep1 : configuration.getBusinessProcesses().getMeps()) {
            if (mep1.getName().equals(this.mepXml)) {
                this.mep = mep1;
                break;
            }
        }
        for (final Binding binding : configuration.getBusinessProcesses().getMepBindings()) {
            if (binding.getName().equals(this.bindingXml)) {
                this.mepBinding = binding;
                break;
            }
        }

    }

    public String getName() {
        return this.name;
    }

    public Set<Party> getInitiatorParties() {
        return this.initiatorParties;
    }

    public Set<Party> getResponderParties() {
        return this.responderParties;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Process)) return false;
        if (!super.equals(o)) return false;

        final Process process = (Process) o;

        if (!name.equals(process.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    public Set<LegConfiguration> getLegs() {
        return this.legs;
    }

    public Role getInitiatorRole() {
        return this.initiatorRole;
    }

    public Role getResponderRole() {
        return this.responderRole;
    }

    public Agreement getAgreement() {
        return this.agreement;
    }

    public Mep getMep() {
        return this.mep;
    }

    public Binding getMepBinding() {
        return this.mepBinding;
    }

    public boolean isDynamicResponder() {
        return dynamicResponder;
    }

    public boolean isDynamicInitiator() {
        return dynamicInitiator;
    }

    public static String getMepValue(Process process) {
        String mepVal = "";
        Mep mep = process.getMep();
        if (mep != null) {
            String name = mep.getName();
            if (name != null) {
                mepVal = name;
            }
        }
        return mepVal;
    }

    public static String getBindingValue(Process process) {
        String bindingVal = "";
        Binding binding = process.getMepBinding();
        if (binding != null) {
            String name = binding.getName();
            if (name != null) {
                bindingVal = name;
            }
        }
        return bindingVal;
    }
}
