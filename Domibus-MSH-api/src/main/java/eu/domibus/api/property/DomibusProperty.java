package eu.domibus.api.property;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 *
 * REST service class for getting the current value of a domibus property along with its metadata
 */
public class DomibusProperty {
    private String value;

    private DomibusPropertyMetadata metadata;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public DomibusPropertyMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(DomibusPropertyMetadata metadata) {
        this.metadata = metadata;
    }
}
