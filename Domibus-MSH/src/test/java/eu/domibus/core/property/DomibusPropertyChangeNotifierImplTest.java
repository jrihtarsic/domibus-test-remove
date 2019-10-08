package eu.domibus.core.property;

import eu.domibus.api.cluster.SignalService;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusPropertyChangeListener;
import eu.domibus.core.property.listeners.BlacklistChangeListener;
import eu.domibus.core.property.listeners.ConcurrencyChangeListener;
import eu.domibus.core.property.listeners.CronExpressionChangeListener;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;

import java.util.Arrays;
import java.util.List;

@RunWith(JMockit.class)
public class DomibusPropertyChangeNotifierImplTest {

    @Injectable
    private List<DomibusPropertyChangeListener> propertyChangeListeners;

    @Injectable
    private SignalService signalService;

    @Tested
    DomibusPropertyChangeNotifierImpl domibusPropertyChangeNotifier;

    @Mocked
    BlacklistChangeListener blacklistChangeListener;

    @Mocked
    ConcurrencyChangeListener concurrencyChangeListener;

    @Mocked
    CronExpressionChangeListener cronExpressionChangeListener;

    @Test
    public void signalPropertyValueChanged() {
        String domainCode = "domain1";
        String propertyName = "prop1";
        String propertyValue = "val";
        boolean broadcast = true;

        domibusPropertyChangeNotifier.propertyChangeListeners = Arrays.asList(
                blacklistChangeListener,
                concurrencyChangeListener,
                cronExpressionChangeListener
        );

        new Expectations() {{
            blacklistChangeListener.handlesProperty(propertyName);
            result = false;
            concurrencyChangeListener.handlesProperty(propertyName);
            result = true;
            cronExpressionChangeListener.handlesProperty(propertyName);
            result = false;
        }};

        domibusPropertyChangeNotifier.signalPropertyValueChanged(domainCode, propertyName, propertyValue, broadcast);

        new Verifications() {{
            concurrencyChangeListener.propertyValueChanged(domainCode, propertyName, propertyValue);
            signalService.signalDomibusPropertyChange(domainCode, propertyName, propertyValue);
        }};
    }
}