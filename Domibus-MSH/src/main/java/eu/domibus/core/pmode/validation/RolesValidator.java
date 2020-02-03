package eu.domibus.core.pmode.validation;

import eu.domibus.api.pmode.PModeIssue;
import eu.domibus.common.model.configuration.BusinessProcesses;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.common.model.configuration.Role;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author musatmi
 * @since 3.3
 */
@Component
@Order(1)
public class RolesValidator extends AbstractPModeValidator {

    @Override
    public List<PModeIssue> validate(Configuration configuration) {
        List<PModeIssue> issues = new ArrayList<>();

        final BusinessProcesses businessProcesses = configuration.getBusinessProcesses();
        for (Process process : businessProcesses.getProcesses()) {
            final Role initiatorRole = process.getInitiatorRole();
            final Role responderRole = process.getResponderRole();
            if (initiatorRole != null && initiatorRole.equals(responderRole)) {
                String errorMessage = "For the business process [" + process.getName() + "], the initiator role name and the responder role name are identical [" + initiatorRole.getName() + "]";
                issues.add(new PModeIssue(errorMessage, PModeIssue.Level.WARNING));
            }
            if (initiatorRole != null && responderRole != null
                    && StringUtils.equalsIgnoreCase(initiatorRole.getValue(), responderRole.getValue())) {
                String errorMessage = "For the business process [" + process.getName() + "], the initiator role value and the responder role value are identical [" + initiatorRole.getValue() + "]";
                issues.add(new PModeIssue(errorMessage, PModeIssue.Level.WARNING));
            }
        }

        return Collections.unmodifiableList(issues);
    }


}