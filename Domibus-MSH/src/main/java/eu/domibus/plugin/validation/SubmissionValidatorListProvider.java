package eu.domibus.plugin.validation;

import eu.domibus.plugin.validation.SubmissionValidatorList;

/**
 * Created by Cosmin Baciu on 04-Aug-16.
 */
public interface SubmissionValidatorListProvider {

    SubmissionValidatorList getSubmissionValidatorList(String backendName);
}
