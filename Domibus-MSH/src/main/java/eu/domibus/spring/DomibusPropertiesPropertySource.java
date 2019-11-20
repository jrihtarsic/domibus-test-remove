package eu.domibus.spring;

import org.springframework.core.env.PropertiesPropertySource;

import java.util.Map;
import java.util.Properties;

public class DomibusPropertiesPropertySource extends PropertiesPropertySource {

    public static final String NAME = "domibusProperties";

    public DomibusPropertiesPropertySource(String name, Properties source) {
        super(name, source);
    }

    protected DomibusPropertiesPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    public void setProperty(String name, String value) {
        this.source.put(name, value);
    }
}