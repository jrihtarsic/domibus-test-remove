package eu.domibus.plugin.fs;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static eu.domibus.plugin.fs.worker.FSSendMessagesService.DEFAULT_DOMAIN;

/**
 * File System Plugin Properties
 *
 * All the plugin configurable properties must be accessed and handled through this component.
 *
 * @author @author FERNANDES Henrique, GONCALVES Bruno
 */
@Component
public class FSPluginProperties {

    private static final String DOT = ".";

    private static final String PROPERTY_PREFIX = "fsplugin.";

    private static final String DOMAIN_PREFIX = "fsplugin.domains.";

    private static final String LOCATION = "messages.location";

    private static final String SENT_ACTION = "messages.sent.action";

    private static final String SENT_PURGE_WORKER_CRONEXPRESSION = "messages.sent.purge.worker.cronExpression";

    private static final String SENT_PURGE_EXPIRED = "messages.sent.purge.expired";

    private static final String FAILED_ACTION = "messages.failed.action";

    private static final String FAILED_PURGE_WORKER_CRONEXPRESSION = "messages.failed.purge.worker.cronExpression";

    private static final String FAILED_PURGE_EXPIRED = "messages.failed.purge.expired";

    private static final String RECEIVED_PURGE_EXPIRED = "messages.received.purge.expired";

    private static final String RECEIVED_PURGE_WORKER_CRONEXPRESSION = "messages.received.purge.worker.cronExpression";

    private static final String USER = "messages.user";

    private static final String PAYLOAD_ID = "messages.payload.id";

    private static final String DEFAULT_CONTENT_ID = "cid:message";

    // Sonar confuses this constant with an actual password
    @SuppressWarnings("squid:S2068")
    private static final String PASSWORD = "messages.password";

    private static final String AUTHENTICATION_USER = "authentication.user";

    // Sonar confuses this constant with an actual password
    @SuppressWarnings("squid:S2068")
    private static final String AUTHENTICATION_PASSWORD = "authentication.password";

    private static final String EXPRESSION = "messages.expression";

    private static final String ORDER = "order";

    @Resource(name = "fsPluginProperties")
    private Properties properties;

    private List<String> domains;

    public static final String ACTION_DELETE = "delete";

    public static final String ACTION_ARCHIVE = "archive";

    /**
     * @return The available domains set
     */
    public List<String> getDomains() {
        if (domains == null) {
            domains = readDomains();
        }
        return domains;
    }

    /**
     * @return The location of the directory that the plugin will use to manage the messages to be sent and received
     * in case no domain expression matches
     */
    public String getLocation() {
        return getLocation(null);
    }

    /**
     * @param domain The domain property qualifier
     * @return See {@link FSPluginProperties#getLocation()}
     */
    public String getLocation(String domain) {
        return getDomainProperty(domain, LOCATION, System.getProperty("java.io.tmpdir"));
    }

    /**
     * @return The plugin action when message is sent successfully from C2 to C3 ('delete' or 'archive')
     */
    public String getSentAction() {
        return getSentAction(null);
    }

    /**
     * @param domain The domain property qualifier
     * @return See {@link FSPluginProperties#getSentAction()}
     */
    public String getSentAction(String domain) {
        return getDomainProperty(domain, SENT_ACTION, ACTION_DELETE);
    }

    /**
     * @return The cron expression that defines the frequency of the sent messages purge job
     */
    public String getSentPurgeWorkerCronExpression() {
        return properties.getProperty(PROPERTY_PREFIX + SENT_PURGE_WORKER_CRONEXPRESSION);
    }

    /**
     * @return The time interval (seconds) to purge sent messages
     */
    public Integer getSentPurgeExpired() {
        return getSentPurgeExpired(null);
    }

    /**
     * @param domain The domain property qualifier
     * @return See {@link FSPluginProperties#getSentPurgeExpired()}
     */
    public Integer getSentPurgeExpired(String domain) {
        String value = getDomainProperty(domain, SENT_PURGE_EXPIRED, "600");
        return StringUtils.isNotEmpty(value) ? Integer.parseInt(value) : null;
    }

    /**
     * @return The plugin action when message fails
     */
    public String getFailedAction() {
        return getFailedAction(null);
    }

    /**
     * @param domain The domain property qualifier
     * @return See {@link FSPluginProperties#getFailedAction()}
     */
    public String getFailedAction(String domain) {
        return getDomainProperty(domain, FAILED_ACTION, ACTION_DELETE);
    }

    /**
     * @return The cron expression that defines the frequency of the failed messages purge job
     */
    public String getFailedPurgeWorkerCronExpression() {
        return properties.getProperty(PROPERTY_PREFIX + FAILED_PURGE_WORKER_CRONEXPRESSION);
    }

    /**
     * @return The time interval (seconds) to purge failed messages
     */
    public Integer getFailedPurgeExpired() {
        return getFailedPurgeExpired(null);
    }

    /**
     * @param domain The domain property qualifier
     * @return See {@link FSPluginProperties#getFailedPurgeExpired()}
     */
    public Integer getFailedPurgeExpired(String domain) {
        String value = getDomainProperty(domain, FAILED_PURGE_EXPIRED, "600");
        return StringUtils.isNotEmpty(value) ? Integer.parseInt(value) : null;
    }

    /**
     * @return The time interval (seconds) to purge received messages
     */
    public Integer getReceivedPurgeExpired() {
        return getReceivedPurgeExpired(null);
    }

    /**
     * @param domain The domain property qualifier
     * @return See {@link FSPluginProperties#getReceivedPurgeExpired()}
     */
    public Integer getReceivedPurgeExpired(String domain) {
        String value = getDomainProperty(domain, RECEIVED_PURGE_EXPIRED, "600");
        return StringUtils.isNotEmpty(value) ? Integer.parseInt(value) : null;
    }

    /**
     * @return The cron expression that defines the frequency of the received messages purge job
     */
    public String getReceivedPurgeWorkerCronExpression() {
        return properties.getProperty(PROPERTY_PREFIX + RECEIVED_PURGE_WORKER_CRONEXPRESSION);
    }

    /**
     * @param domain The domain property qualifier
     * @return the user used to access the location specified by the property
     */
    public String getUser(String domain) {
        return getDomainProperty(domain, USER, null);
    }

    /**
     * Returns the payload identifier for messages belonging to a particular domain or the default payload identifier if none is defined.
     *
     * @param domain the domain property qualifier; {@code null} for the non-multitenant default domain
     *
     * @return The identifier used to reference payloads of messages belonging to a particular domain.
     */
    public String getPayloadId(String domain) {
        return getDomainProperty(domain, PAYLOAD_ID, DEFAULT_CONTENT_ID);
    }

    /**
     * @param domain The domain property qualifier
     * @return the password used to access the location specified by the property
     */
    public String getPassword(String domain) {
        return getDomainProperty(domain, PASSWORD, null);
    }

    /**
     * @param domain The domain property qualifier
     * @return the user used to authenticate
     */
    public String getAuthenticationUser(String domain) {
        return getDomainPropertyNoDefault(domain, AUTHENTICATION_USER, null);
    }

    /**
     * @param domain The domain property qualifier
     * @return the password used to authenticate
     */
    public String getAuthenticationPassword(String domain) {
        return getDomainPropertyNoDefault(domain, AUTHENTICATION_PASSWORD, null);
    }

    /**
     * @param domain The domain property qualifier
     * @return the domain order
     */
    public Integer getOrder(String domain) {
        String value = getDomainProperty(domain, ORDER, null);
        return getInteger(value, Integer.MAX_VALUE);
    }

    Properties getProperties() {
        return properties;
    }

    void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * @param domain The domain property qualifier
     * @return the regex expression used to determine the domain location
     */
    public String getExpression(String domain) {
        return getDomainProperty(domain, EXPRESSION, null);
    }

    /**
     * @param domain The domain property qualifier
     * @return True if the sent messages action is "archive"
     */
    public boolean isSentActionArchive(String domain) {
        return ACTION_ARCHIVE.equals(getSentAction(domain));
    }

    /**
     * @param domain The domain property qualifier
     * @return True if the sent messages action is "delete"
     */
    public boolean isSentActionDelete(String domain) {
        return ACTION_DELETE.equals(getSentAction(domain));
    }

    /**
     * @param domain The domain property qualifier
     * @return True if the send failed messages action is "archive"
     */
    public boolean isFailedActionArchive(String domain) {
        return ACTION_ARCHIVE.equals(getFailedAction(domain));
    }

    /**
     * @param domain The domain property qualifier
     * @return True if the send failed messages action is "delete"
     */
    public boolean isFailedActionDelete(String domain) {
        return ACTION_DELETE.equals(getFailedAction(domain));
    }

    private String getDomainProperty(String domain, String propertyName, String defaultValue) {
        String domainFullPropertyName = getDomainPropertyName(domain, propertyName);
        if (properties.containsKey(domainFullPropertyName)) {
            return properties.getProperty(domainFullPropertyName, defaultValue);
        }
        return properties.getProperty(PROPERTY_PREFIX + propertyName, defaultValue);
    }

    private String getDomainPropertyNoDefault(String domain, String propertyName, String defaultValue) {
        String domainFullPropertyName = getDomainPropertyName(domain, propertyName);
        if (properties.containsKey(domainFullPropertyName)) {
            return properties.getProperty(domainFullPropertyName, defaultValue);
        }
        if(DEFAULT_DOMAIN.equals(domain)) {
            return properties.getProperty(PROPERTY_PREFIX + propertyName, defaultValue);
        }
        // cannot use default values
        return null;
    }

    protected String getDomainPropertyName(String domain, String propertyName) {
        return DOMAIN_PREFIX + domain + DOT + propertyName;
    }

    private Integer getInteger(String value, Integer defaultValue) {
        Integer result = defaultValue;
        if (StringUtils.isNotEmpty(value)) {
            result = Integer.valueOf(value);
        }
        return result;
    }

    private List<String> readDomains() {
        List<String> tempDomains = new ArrayList<>();

        for (String propName : properties.stringPropertyNames()) {
            if (propName.startsWith(DOMAIN_PREFIX)) {
                String domain = extractDomainName(propName);
                if (!tempDomains.contains(domain)) {
                    tempDomains.add(domain);
                }
            }
        }

        Collections.sort(tempDomains, (domain1, domain2) -> {
            Integer domain1Order = getOrder(domain1);
            Integer domain2Order = getOrder(domain2);
            return domain1Order - domain2Order;
        });
        return tempDomains;
    }

    private String extractDomainName(String propName) {
        String unprefixedProp = StringUtils.removeStart(propName, DOMAIN_PREFIX);
        return StringUtils.substringBefore(unprefixedProp, ".");
    }

}
