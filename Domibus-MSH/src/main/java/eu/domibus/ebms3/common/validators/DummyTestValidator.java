package eu.domibus.ebms3.common.validators;

import eu.domibus.common.model.configuration.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @author musatmi
 * @since 3.3
 */
@Component
@Order(0)
public class DummyTestValidator implements ConfigurationValidator {
    @Override
    public List<String> validate(Configuration configuration) {
        return Collections.emptyList();
    }
}
