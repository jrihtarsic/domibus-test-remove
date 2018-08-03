package eu.domibus.plugin.routing;

import eu.domibus.ebms3.common.model.UserMessage;

/**
 * Routing Interface for incoming user messages
 *
 * @author Christian Walczac
 */
public interface IRoutingCriteria {

    /**
     * Returns if $UserMessage matches expression
     *
     * @param candidate user message to match
     * @param expression expression to match
     * @return boolean result
     */
    public boolean matches(UserMessage candidate, String expression);

    /**
     * Returns name of Routing Criteria
     *
     * @return name of Routing Criteria
     */
    public String getName();

    String getInputPattern();

    void setInputPattern(String inputPattern);

    String getTooltip();

    void setTooltip(String tooltip);

    public String getExpression();

    public void setExpression(String expression);


}
