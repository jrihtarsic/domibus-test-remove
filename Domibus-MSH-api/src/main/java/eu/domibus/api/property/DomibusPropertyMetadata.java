package eu.domibus.api.property;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * Class that encapsulates the properties of a domibus configuration property;
 */
public class DomibusPropertyMetadata {

    public boolean isOnlyGlobal() {
        return getUsage() == Usage.GLOBAL;
    }

    public boolean isGlobal() {
        return (getUsage() & Usage.GLOBAL) == Usage.GLOBAL;
    }

    public boolean isSuper() {
        return (getUsage() & Usage.SUPER) == Usage.SUPER;
    }

    public boolean isDomain() {
        return (getUsage() & Usage.DOMAIN) == Usage.DOMAIN;
    }

    /**
     * The name of the property, corresponds to the value in the domibus.properties files
     * ex: domibus.ui.replication.sync.cron, domibus.ui.replication.sync.cron.max.rows
     */
    private String name;

    /**
     * The technical type of the property which dictates the way it is handled at runtime
     * ex:  numeric, cron expr, regexp, string, concurrency
     */
    private String type;

    /**
     * When GLOBAL, it means global property. When DOMAIN, it means domain property, when SUPER, it means for super-users
     * In single tenancy mode, a global property can be changed by regular admins.
     * In multi tenancy mode, a global property can be changed only by AP admins.
     */
    private int usage;

    /**
     * If it can be suffixed with different sufixes
     */
    private boolean isComposable;

    /**
     * For domain properties(which also means super properties), this flag specifies whether the value is read
     * from the global properties file(which contains also default values for domain properties) if not found in the current domain.
     */
    private boolean withFallback;

    /**
     * When true, the property must be changed across the cluster machines, so a notification is raised
     */
    private boolean clusterAware;

    /**
     * Corresponds to the existing sections in domibus.properties files: Cluster, GUI, Keystore/Truststore
     */
    private String section;

    /**
     * Corresponds to the existing descriptions of properties found in domibus.properties files
     * ex: #The location of the keystore
     */
    private String description;

    /**
     * One of Module class constants: MSH, WS_PLUGIN, JMS_PLUGIN, FS_PLUGIN but can take other values for new plugins or external modules
     */
    private String module;

    private boolean writable;

    private boolean encrypted;

    public static DomibusPropertyMetadata getGlobalProperty(String name) {
        return getGlobalProperty(name, Type.STRING);
    }

    public static DomibusPropertyMetadata getGlobalProperty(String name, Type type) {
        DomibusPropertyMetadata res = new DomibusPropertyMetadata(name, Usage.GLOBAL, false);
        res.setType(type.name());
        return res;
    }

    public static DomibusPropertyMetadata getReadOnlyGlobalProperty(String name) {
        return getReadOnlyGlobalProperty(name, Type.STRING);
    }

    public static DomibusPropertyMetadata getReadOnlyGlobalProperty(String name, Type type) {
        DomibusPropertyMetadata res = new DomibusPropertyMetadata(name, Module.MSH, false, Usage.GLOBAL, false, false, false, false);
        res.setType(type.name());
        return res;
    }

    public static DomibusPropertyMetadata getReadOnlyGlobalProperty(String name, boolean encrypted) {
        return new DomibusPropertyMetadata(name, Module.MSH, false, Usage.GLOBAL, false, false, encrypted, false);
    }

    public static DomibusPropertyMetadata getReadOnlyGlobalProperty(String name, String module) {
        return new DomibusPropertyMetadata(name, module, false, Usage.GLOBAL, false, false, false, false);
    }

    public static DomibusPropertyMetadata getReadOnlyGlobalProperty(String name, Type type, String module) {
        return new DomibusPropertyMetadata(name, type, module, false, Usage.GLOBAL, false, false, false, false);
    }

    public DomibusPropertyMetadata() {
    }

    public DomibusPropertyMetadata(String name, String module, boolean writable, int usage, boolean withFallback, boolean clusterAware, boolean encrypted, boolean isComposable) {
        this(name, Type.STRING, module, writable, usage, withFallback, clusterAware, encrypted, isComposable);
    }

    public DomibusPropertyMetadata(String name, Type type, String module, boolean writable, int usage, boolean withFallback, boolean clusterAware, boolean encrypted, boolean isComposable) {
        this.name = name;
        this.type = type.name();
        this.writable = writable;
        this.usage = usage;
        this.withFallback = withFallback;
        this.clusterAware = clusterAware;
        this.module = module;
        this.encrypted = encrypted;
        this.isComposable = isComposable;
    }

    public DomibusPropertyMetadata(String name, Type type, int usage, boolean withFallback) {
        this(name, type, Module.MSH, true, usage, withFallback, true, false, false);
    }

    public DomibusPropertyMetadata(String name, int usage, boolean withFallback) {
        this(name, Module.MSH, true, usage, withFallback, true, false, false);
    }

    public DomibusPropertyMetadata(String name, Type type, boolean writable, int usage, boolean withFallback) {
        this(name, type, Module.MSH, writable, usage, withFallback, true, false, false);
    }

    public DomibusPropertyMetadata(String name, boolean writable, int usage, boolean withFallback) {
        this(name, Module.MSH, writable, usage, withFallback, true, false, false);
    }

    public DomibusPropertyMetadata(String name, boolean writable, int usage, boolean withFallback, boolean encrypted) {
        this(name, Module.MSH, writable, usage, withFallback, true, encrypted, false);
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

    public int getUsage() {
        return usage;
    }

    public void setUsage(int usage) {
        this.usage = usage;
    }

    public boolean isComposable() {
        return isComposable;
    }

    public void setComposable(boolean composable) {
        isComposable = composable;
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

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getUsageText() {
        switch (usage) {
            case 1:
                return "Global";
            case 2:
                return "Domain";
            case 3:
                return "Global and Domain";
            case 4:
                return "Super";
            case 6:
                return "Domain and Super";
            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        DomibusPropertyMetadata el = (DomibusPropertyMetadata) o;

        return new EqualsBuilder()
                .append(name, el.getName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .toHashCode();
    }

    public class Usage {
        public static final int GLOBAL = 1;
        public static final int DOMAIN = 2;
        public static final int SUPER = 4;
        public static final int GLOBAL_AND_DOMAIN = GLOBAL | DOMAIN;
        public static final int DOMAIN_AND_SUPER = DOMAIN | SUPER;
    }

    public enum Type {
        NUMERIC("^(\\d+)$"),
        BOOLEAN("^(true|false)$"),
        STRING(null),
        CRON("^(\\*|([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])|\\*\\/([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])) (\\*|([0-9]|1[0-9]|2[0-3])|\\*\\/([0-9]|1[0-9]|2[0-3])) (\\*|([1-9]|1[0-9]|2[0-9]|3[0-1])|\\*\\/([1-9]|1[0-9]|2[0-9]|3[0-1])) (\\*|([1-9]|1[0-2])|\\*\\/([1-9]|1[0-2])) (\\*|([0-6])|\\*\\/([0-6]))$"),
        CONCURRENCY("^(\\d+(\\-\\d+)*)$"),
        EMAIL("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+.[a-zA-Z]{1,}$");

        private final String regularExpression;

        Type(String regularExpression) {
            this.regularExpression = regularExpression;
        }

        public String getRegularExpression() {
            return regularExpression;
        }
    }
}
