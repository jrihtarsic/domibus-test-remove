package eu.domibus.common.services.impl;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.Process;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;

import java.util.Collection;

/**
 * @author Thomas Dussart
 * @since 3.3
 * <p>
 * Contextual class for a pull.
 */
public class PullContext {

    private Process process;
    private Party responder;
    private Party initiator;
    private String mpcQualifiedName;
    public static final String MPC = "mpc";
    public static final String PMODE_KEY = "pmodKey";
    public static final String NOTIFY_BUSINNES_ON_ERROR = "NOTIFY_BUSINNES_ON_ERROR";

    public PullContext(final Process process, final Party responder, final String mpcQualifiedName) {
        Validate.notNull(process);
        Validate.notNull(responder);
        Validate.notNull(mpcQualifiedName);
        this.process = process;
        this.mpcQualifiedName = mpcQualifiedName;
        this.responder = responder;
        this.initiator = null;
    }

    public PullContext(final Process process, final Party responder, final Party initiator, final String mpcQualifiedName) {
        Validate.notNull(process);
        Validate.notNull(responder);
        Validate.notNull(mpcQualifiedName);
        this.process = process;
        this.mpcQualifiedName = mpcQualifiedName;
        this.responder = responder;
        this.initiator = initiator;
    }


    public String getAgreement() {
        if (process.getAgreement() != null) {
            return process.getAgreement().getName();
        }
        return "";
    }

    public Process getProcess() {
        return process;
    }

    public Party getInitiator() {
        if(initiator != null) {
            return initiator;
        }
        if(CollectionUtils.isEmpty(process.getInitiatorParties())) {
            return null;
        }
        return process.getInitiatorParties().iterator().next();
    }

    public String getMpcQualifiedName() {
        return mpcQualifiedName;
    }

    public Party getResponder() {
        return responder;
    }

    public LegConfiguration filterLegOnMpc() {
        return filterLegOnMpc(mpcQualifiedName);
    }

    public LegConfiguration filterLegOnMpc(String mpc) {
        if (mpc != null) {
            Collection<LegConfiguration> filter = Collections2.filter(process.getLegs(), new Predicate<LegConfiguration>() {
                @Override
                public boolean apply(LegConfiguration legConfiguration) {
                    return mpc.equalsIgnoreCase(legConfiguration.getDefaultMpc().getQualifiedName());
                }
            });
            return filter.iterator().next();
        } else throw new IllegalArgumentException("Method should be called after correct context setup.");
    }



}
