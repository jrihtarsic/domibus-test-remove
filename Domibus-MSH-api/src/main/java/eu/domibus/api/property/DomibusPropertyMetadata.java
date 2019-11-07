package eu.domibus.api.property;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * Class that encapsulates the properties of a domibus configuration property;
 */
public class DomibusPropertyMetadata {

    /**
     * The name of the property, corresponds to the value in the domibus.properties files
     * ex: domibus.ui.replication.sync.cron, domibus.ui.replication.sync.cron.max.rows
     */
    private String name;

    /**
     *  The technical type of the property which dictates the way it is handled at runtime
     *  ex:  numeric, cron expr, regexp, string, concurrency
     */
    private String type;

    /**
     * When false, it means global property. When true, it means domain property.
     * In single tenancy mode, a global property can be changed by regular admins.
     * In multi tenancy mode, a global property can be changed only by AP admins.
     */
    private boolean domainSpecific;

    /**
     * For domain properties, this flag specifies whether the value is read
     * from the default domain if not found in the current domain.
     * This is subject to change in the near future.
     */
    private boolean withFallback;

    /**
     * When true, pre property must be changed across the cluster machines, so a notification is raised
     */
    private boolean clusterAware;

    /**
     * Corresponds to the existing sections in domibus.properties files: Cluster, GUI, Keystore/Truststore
     */
    private String section;

    /**
     * Correspondts to the existing descriptions of properties found in domibus.properties files
     * ex: #The location of the keystore
     */
    private String description;

    /**
     * One of Module class constants: MSH, WS_PLUGIN, JMS_PLUGIN, FS_PLUGIN but can take other values for new plugins or external modules
     */
    private String module;

    public static DomibusPropertyMetadata getGlobalProperty(String name) {
        return new DomibusPropertyMetadata(name, false, false);
    }

    public DomibusPropertyMetadata() {
    }

    public DomibusPropertyMetadata(String name, String module, boolean domainSpecific, boolean withFallback, boolean clusterAware) {
        this.name = name;
        this.domainSpecific = domainSpecific;
        this.withFallback = withFallback;
        this.clusterAware = clusterAware;
        this.module = module;
    }

    public DomibusPropertyMetadata(String name, boolean domainSpecific, boolean withFallback) {
        this(name, Module.MSH, domainSpecific, withFallback, true);
    }

    public DomibusPropertyMetadata(String name, boolean domainSpecific) {
        this(name, Module.MSH, domainSpecific, false, true);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDomainSpecific() {
        return domainSpecific;
    }

    public void setDomainSpecific(boolean domainSpecific) {
        this.domainSpecific = domainSpecific;
    }

    public boolean isWithFallback() {
        return withFallback;
    }

    public void setWithFallback(boolean withFallback) {
        this.withFallback = withFallback;
    }

    public boolean isClusterAware() {
        return clusterAware;
    }

    public void setClusterAware(boolean clusterAware) {
        this.clusterAware = clusterAware;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }
}
