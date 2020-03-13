package eu.domibus.plugin.routing;

import eu.domibus.ebms3.common.model.UserMessage;
import org.springframework.stereotype.Service;

/**
 * @author Christian Walczac
 */

@Service
public class ActionRoutingCriteriaFactory implements CriteriaFactory {

    private static final String NAME = "ACTION";
    private static final String TOOLTIP = "Type in the filtering rule: [ACTION]. Combine with regular expression.";
    private static final String INPUTPATTERN = "\\\\w+";

    @Override
    public IRoutingCriteria getInstance() {
        return new ActionRoutingCriteriaEntity(NAME, TOOLTIP, INPUTPATTERN);
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
        return INPUTPATTERN;
    }


    private class ActionRoutingCriteriaEntity extends RoutingCriteriaEntity {

        private ActionRoutingCriteriaEntity(final String name, final String tooltip, final String inputPattern) {
            super(name, tooltip, inputPattern);
        }

        @Override
        public boolean matches(final UserMessage userMessage, final String expression) {
            setExpression(expression);
            return super.matches(userMessage.getCollaborationInfo().getAction());
        }
    }
}