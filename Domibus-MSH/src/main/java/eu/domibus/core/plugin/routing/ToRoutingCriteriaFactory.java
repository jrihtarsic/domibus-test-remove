package eu.domibus.core.plugin.routing;

import eu.domibus.ebms3.common.model.PartyId;
import eu.domibus.ebms3.common.model.UserMessage;
import org.springframework.stereotype.Service;

/**
 * To criteria for user messages
 *
 * @author Christian Walczac
 */
@Service
public class ToRoutingCriteriaFactory implements CriteriaFactory {

    private static final String NAME = "TO";
    private static final String TOOLTIP = "Type in the filtering rule: [PARTYID]:[TYPE]. Combine with regular expression.";
    private static final String INPUTPATTERN = "\\\\w+[:]\\\\w+";

    @Override
    public IRoutingCriteria getInstance() {
        return new ToRoutingCriteriaEntity(NAME, TOOLTIP, INPUTPATTERN);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getTooltip() {
        return TOOLTIP;
    }

    @Override
    public String getInputPattern() {
        return null;
    }

    private class ToRoutingCriteriaEntity extends RoutingCriteriaEntity implements IRoutingCriteria {

        private ToRoutingCriteriaEntity(final String name, final String tooltip, final String inputPattern) {
            super(name, tooltip, inputPattern);
        }

        @Override
        public boolean matches(final UserMessage userMessage, final String expression) {
            setExpression(expression);
            for (final PartyId partyId : userMessage.getPartyInfo().getTo().getPartyId()) {
                if (matches(partyId.getValue() + ":" + partyId.getType())) {
                    return true;
                }
            }
            return false;
        }

    }

}
