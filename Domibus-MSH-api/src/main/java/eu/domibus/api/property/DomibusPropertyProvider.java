package eu.domibus.api.property;

import eu.domibus.api.multitenancy.Domain;

import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
public interface DomibusPropertyProvider {

    String DOMIBUS_PROPERTY_FILE = "domibus.properties";

    String getProperty(String propertyName);

//    String getProperty(String propertyName, boolean decrypt);

//    String getProperty(Domain domain, String propertyName);

//    String getProperty(Domain domain, String propertyName, boolean decrypt);


    /*
    The getDomainProperty methods retrieve the specified property
    falling back to the property from the DEFAULT domain if not found.
     */

    /**
     * Look for a property in the active domain configuration file. If the property is not found, it will search for the property in
     * the following locations and in the respective order:
     * conf/domibus.properties, classpath://domibus.properties, classpath://domibus-default.properties

     * When actions are executed under a super admin user, there is no domain set on the current thread.
     * Nevertheless we need to retrieve some default properties. So if no domain is found, this method will retrieve
     * properties from the default one.
     *
     * @param propertyName the property name.
     * @return the value for that property.
     */
//    String getDomainProperty(String propertyName);

    String getProperty(Domain domain, String propertyName);

    Set<String> filterPropertiesName(Predicate<String> predicate);

    /**
     * <p>Reads a property value inside the {@link eu.domibus.api.multitenancy.DomainService#DEFAULT_DOMAIN DEFAULT} domain and parses it safely as an {@code Integer} before
     * returning it.</p><br />
     *
     * <p>If the value is not found in the users files, the default value is then being returned from the domibus-default.properties and its corresponding server-specific
     * domibus.properties files that are provided with the application.</p>
     *
     * @param propertyName the property name.
     * @return The {@code Integer} value of the property as specified by the user or the default one provided with the application.
     */
//    Integer getIntegerProperty(String propertyName);

    /**
     * <p>Reads a domain property value and parses it safely as an {@code Integer} before returning it.</p><br />
     *
     * <p>If the value is not found in the users files, the default value is then being returned from the domibus-default.properties and its corresponding server-specific
     * domibus.properties files that are provided with the application.</p>
     *
     * @param propertyName the property name.
     * @return The {@code Integer} value of the domain property as specified by the user or the default one provided with the application.
     */
    Integer getIntegerProperty(String propertyName);

//    Integer getIntegerProperty(Domain domain, String propertyName);

    Long getLongProperty(String propertyName);

    /**
     * <p>Reads a property value inside the {@link eu.domibus.api.multitenancy.DomainService#DEFAULT_DOMAIN DEFAULT} domain and parses it safely as a {@code Boolean} before
     * returning it.</p><br />
     *
     * <p>If the value is not found in the users files, the default value is then being returned from the domibus-default.properties and its corresponding server-specific
     * domibus.properties files that are provided with the application.</p>
     *
     * @param propertyName the property name.
     * @return The {@code Boolean} value of the property as specified by the user or the default one provided with the application.
     */
    Boolean getBooleanProperty(String propertyName);

    /**
     * <p>Reads a domain property value and parses it safely as a {@code Boolean} before returning it.</p><br />
     *
     * <p>If the value is not found in the users files, the default value is then being returned from the domibus-default.properties and its corresponding server-specific
     * domibus.properties files that are provided with the application.</p>
     *
     * @param propertyName the property name.
     * @return The {@code Boolean} value of the domain property as specified by the user or the default one provided with the application.
     */
//    Boolean getBooleanDomainProperty(String propertyName);

    Boolean getBooleanProperty(Domain domain, String propertyName);

    /**
     * Verify that a property key exists within a domain configuration whether it is empty or not.
     * If not found, the property will be looked within the domibus/default-domain properties
     *
     * @param domain       the domain.
     * @param propertyName the property name.
     * @return true if the property exists.
     */
    boolean containsDomainPropertyKey(Domain domain, String propertyName);

    /**
     * Verify that a property key exists within the domibus/default-domain properties.
     *
     * @param propertyName the name of the property
     * @return true if the property exists.
     */
    boolean containsPropertyKey(String propertyName);

    /**
     * Changes the value of the given property key.
     * @param domain the domain of the property
     * @param propertyName the name of the property
     * @param propertyValue the new value of the property
     */
    void setPropertyValue(Domain domain, String propertyName, String propertyValue);

}
