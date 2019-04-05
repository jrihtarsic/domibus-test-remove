package eu.domibus.common.validators;

import eu.domibus.api.configuration.DomibusConfigurationService;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Property;
import eu.domibus.common.model.configuration.PropertySet;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.ebms3.common.UserMessageServiceHelper;
import eu.domibus.ebms3.common.model.MessageProperties;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Christian Koch, Stefan Mueller
 * @version 3.0
 * @since 3.0
 */

@Service
@Transactional(propagation = Propagation.SUPPORTS)
public class PropertyProfileValidator {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PropertyProfileValidator.class);

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    UserMessageServiceHelper userMessageDefaultServiceHelper;

    @Autowired
    DomibusConfigurationService domibusConfigurationService;

    public void validate(final Messaging messaging, final String pmodeKey) throws EbMS3Exception {
        final List<Property> modifiablePropertyList = new ArrayList<>();
        final LegConfiguration legConfiguration = this.pModeProvider.getLegConfiguration(pmodeKey);
        final PropertySet propSet = legConfiguration.getPropertySet();
        if (propSet == null || CollectionUtils.isEmpty(propSet.getProperties())) {
            LOG.businessInfo(DomibusMessageCode.BUS_PROPERTY_PROFILE_VALIDATION_SKIP, legConfiguration.getName());
            // no profile means everything is valid
            return;
        }

        final Set<Property> profile = propSet.getProperties();

        modifiablePropertyList.addAll(profile);
        eu.domibus.ebms3.common.model.MessageProperties messageProperties = new MessageProperties();
        if(messaging.getUserMessage().getMessageProperties() != null) {
            messageProperties = messaging.getUserMessage().getMessageProperties();
        }

        for (final eu.domibus.ebms3.common.model.Property property : messageProperties.getProperty()) {
            Property profiled = null;
            for (final Property profiledProperty : modifiablePropertyList) {
                if (profiledProperty.getKey().equalsIgnoreCase(property.getName())) {
                    profiled = profiledProperty;
                    break;
                }
            }
            modifiablePropertyList.remove(profiled);
            if (profiled == null) {
                LOG.businessError(DomibusMessageCode.BUS_PROPERTY_MISSING, property.getName());
                throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "Property profiling for this exchange does not include a property named [" + property.getName() + "]", messaging.getUserMessage().getMessageInfo().getMessageId(), null);
            }

            switch (profiled.getDatatype().toLowerCase()) {
                case "string":
                    break;
                case "int":
                    try {
                        Integer.parseInt(property.getValue()); //NOSONAR: Validation is done via exception
                        break;
                    } catch (final NumberFormatException e) {
                        throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "Property profiling for this exchange requires a INTEGER datatype for property named: " + property.getName() + ", but got " + property.getValue(), messaging.getUserMessage().getMessageInfo().getMessageId(), null);
                    }
                case "boolean":
                    if (property.getValue().equalsIgnoreCase("false") || property.getValue().equalsIgnoreCase("true")) {
                        break;
                    }
                    throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "Property profiling for this exchange requires a BOOLEAN datatype for property named: " + property.getName() + ", but got " + property.getValue(), messaging.getUserMessage().getMessageInfo().getMessageId(), null);
                default:
                    PropertyProfileValidator.LOG.warn("Validation for Datatype " + profiled.getDatatype() + " not possible. This type is not known by the validator. The value will be accepted unchecked");
            }


        }
        for (final Property property : modifiablePropertyList) {
            if (property.isRequired()) {
                LOG.businessError(DomibusMessageCode.BUS_PROPERTY_MISSING, property.getName());
                throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "Required property missing [" + property.getName() + "]", messaging.getUserMessage().getMessageInfo().getMessageId(), null);
            }
        }

        LOG.businessInfo(DomibusMessageCode.BUS_PROPERTY_PROFILE_VALIDATION, propSet.getName());
    }
}
