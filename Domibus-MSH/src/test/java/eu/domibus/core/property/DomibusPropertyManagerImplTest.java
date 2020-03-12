package eu.domibus.core.property;

import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.exceptions.DomibusPropertyException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyChangeNotifier;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManager.*;

@RunWith(JMockit.class)
public class DomibusPropertyManagerImplTest {

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private DomainService domainService;

    @Injectable
    private DomibusConfigurationService domibusConfigurationService;

    @Injectable
    private DomibusPropertyChangeNotifier propertyChangeNotifier;

    @Injectable
    DomibusPropertyMetadataManagerImpl domibusPropertyMetadataManager;

    @Injectable
    DomainContextProvider domainContextProvider;

    @Tested
    DomibusPropertyManagerImpl domibusPropertyManager;

    Map<String, DomibusPropertyMetadata> props;
    String domainCode = "domain1";
    Domain domain = new Domain(domainCode, "DomainName1");

    @Before
    public void setUp() {
        props = Arrays.stream(new DomibusPropertyMetadata[]{
                new DomibusPropertyMetadata(DOMIBUS_UI_TITLE_NAME, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_UI_REPLICATION_ENABLED, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_UI_SUPPORT_TEAM_NAME, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_UI_SUPPORT_TEAM_EMAIL, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN, DomibusPropertyMetadata.Usage.DOMAIN, false),
                DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_ALERT_SENDER_SMTP_PORT),
        }).collect(Collectors.toMap(x -> x.getName(), x -> x));
    }

    @Test
    public void getKnownProperties() {
        new Expectations() {{
            domibusPropertyMetadataManager.getKnownProperties();
            result = props;
        }};

        Map<String, DomibusPropertyMetadata> actual = domibusPropertyManager.getKnownProperties();

        Assert.assertEquals(props, actual);
    }

    @Test
    public void hasKnownProperty() {
        new Expectations() {{
            domibusPropertyMetadataManager.hasKnownProperty(DOMIBUS_UI_TITLE_NAME);
            result = true;
        }};

        boolean actual = domibusPropertyManager.hasKnownProperty(DOMIBUS_UI_TITLE_NAME);

        Assert.assertEquals(true, actual);
    }

    @Test(expected = DomibusPropertyException.class)
    public void getKnownPropertyValue_nonExisting() {
        new Expectations() {{
            domibusPropertyMetadataManager.hasKnownProperty("nonExistingPropName");
            result = false;
        }};

        String actual = domibusPropertyManager.getKnownPropertyValue("nonExistingPropName");
    }

    @Test
    public void getKnownPropertyValue_global() {
        DomibusPropertyMetadata meta = props.get(DOMIBUS_ALERT_SENDER_SMTP_PORT);
        String propValue = "propValue";

        new Expectations() {{
            domibusPropertyMetadataManager.hasKnownProperty(DOMIBUS_ALERT_SENDER_SMTP_PORT);
            result = true;

            domibusPropertyProvider.getProperty(meta.getName());
            result = propValue;
        }};

        String actual = domibusPropertyManager.getKnownPropertyValue(DOMIBUS_ALERT_SENDER_SMTP_PORT);

        Assert.assertEquals(propValue, actual);
    }

    @Test
    public void getKnownPropertyValue_domainPropNoFallback() {
        DomibusPropertyMetadata meta = props.get(DOMIBUS_UI_TITLE_NAME);
        String propValue = "propValue";

        new Expectations() {{
            domibusPropertyMetadataManager.hasKnownProperty(DOMIBUS_UI_TITLE_NAME);
            result = true;

            domibusPropertyProvider.getProperty((Domain)any, meta.getName());
            result = propValue;
        }};

        String actual = domibusPropertyManager.getKnownPropertyValue(domainCode, DOMIBUS_UI_TITLE_NAME);

        Assert.assertEquals(propValue, actual);
    }

    @Test
    public void getKnownPropertyValue_domainPropWithFallback() {
        DomibusPropertyMetadata meta = props.get(DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN);
        String propValue = "propValue";

        new Expectations() {{
            domibusPropertyMetadataManager.hasKnownProperty(DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN);
            result = true;

            domibusPropertyProvider.getProperty((Domain)any, meta.getName());
            result = propValue;
        }};

        String actual = domibusPropertyManager.getKnownPropertyValue(domainCode, DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN);

        Assert.assertEquals(propValue, actual);
    }


    @Test(expected = DomibusPropertyException.class)
    public void setKnownPropertyValue_nonExisting() {
        new Expectations() {{
            domibusPropertyMetadataManager.getKnownProperties();
            result = props;
        }};

        domibusPropertyManager.setKnownPropertyValue(domainCode, "nonExistingPropertyName", "val1", true);
    }

    @Test
    public void setKnownPropertyValue() {
        String propValue = "propValue";

        new Expectations() {{
            domibusPropertyMetadataManager.getKnownProperties();
            result = props;

            domainService.getDomain(domainCode);
            result = domain;
        }};

        domibusPropertyManager.setKnownPropertyValue(domainCode, DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN, propValue, true);

        new Verifications() {{
            domibusPropertyProvider.setPropertyValue(domain, DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN, propValue);
            propertyChangeNotifier.signalPropertyValueChanged(domainCode, DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN, propValue, true);
        }};
    }
}