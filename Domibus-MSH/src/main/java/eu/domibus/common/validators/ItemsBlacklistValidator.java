package eu.domibus.common.validators;

import eu.domibus.web.rest.validators.ItemsWhiteListed;
import org.springframework.stereotype.Component;

/**
 * Custom validator that checks that all Strings in the array do not contain any char from the blacklist
 *
 * @author Ion Perpegel
 * @since 4.1
 */
@Component
public class ItemsBlacklistValidator extends BaseBlacklistValidator<ItemsWhiteListed, String[]> {

    @Override
    protected String getErrorMessage() {
        return ItemsWhiteListed.MESSAGE;
    }

    @Override
    public boolean isValid(String[] value) {
        return super.isValidValue(value);
    }

}
