package eu.domibus.ext.domain;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * <p>
 * It is the plugin api equivalent of the DomibusPropertyMetadata
 * Class that encapsulates the properties of a domibus configuration property;
 */
public class DomibusPropertyMetadataDTO {
    public boolean isGlobal() {
        return (getUsage() & Usage.GLOBAL) == Usage.GLOBAL;
    }

    public boolean isSuper() {
        return (getUsage() & Usage.SUPER) == Usage.SUPER;
    }

    public boolean isDomain() {
        return (getUsage() & Usage.DOMAIN) == Usage.DOMAIN;
    }

    public class Usage {
        public static final int GLOBAL = 1;
        public static final int DOMAIN = 2;
        public static final int SUPER = 4;
        public static final int GLOBAL_AND_DOMAIN = GLOBAL | DOMAIN;
        public static final int DOMAIN_AND_SUPER = DOMAIN | SUPER;
    }

    private String name;

    private String type; // numeric, cronexp, regexp, string, concurrency

    /**
     * When false, it means global property. When true, it means domain property.
     * In single tenancy mode, a global property can be changed by regular admins.
     * In multi tenancy mode, a global property can be changed only by AP admins.
     */
    private boolean domainSpecific;

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
     * For domain properties, this flag specifies whether the value is read
     * from the default domain if not found in the current domain.
     * This is subject to change in the near future.
     */
    private boolean withFallback;

    private boolean clusterAware;

    private String section;

    private String description;

    private String module;

    private boolean writable;

    private boolean encrypted;

    public DomibusPropertyMetadataDTO() {
    }

    public DomibusPropertyMetadataDTO(String name, String module, boolean writable, int usage, boolean withFallback, boolean clusterAware, boolean encrypted, boolean isComposable) {
        this.name = name;
        this.writable = writable;
        this.usage = usage;
        this.withFallback = withFallback;
        this.clusterAware = clusterAware;
        this.module = module;
        this.encrypted = encrypted;
        this.isComposable = isComposable;
    }

    public DomibusPropertyMetadataDTO(String name, String module, int usage) {
        this(name, module, true, usage, false, true, false, false);
    }

    public DomibusPropertyMetadataDTO(String name, String module, int usage, boolean withFallback) {
        this(name, module, true, usage, withFallback, true, false, false);
    }

    public DomibusPropertyMetadataDTO(String name, int usage, boolean withFallback) {
        this(name, Module.MSH, true, usage, withFallback, true, false, false);
    }

    public DomibusPropertyMetadataDTO(String name) {
        this(name, Module.MSH, true, Usage.DOMAIN, true, true, false, false);
    }

    @Deprecated
    public DomibusPropertyMetadataDTO(String name, String module, boolean domainSpecific, boolean withFallback) {
        this(name, module, domainSpecific ? DomibusPropertyMetadataDTO.Usage.DOMAIN : Usage.GLOBAL, withFallback);
    }
    @Deprecated
    public DomibusPropertyMetadataDTO(String name, String module, boolean domainSpecific) {
        this(name, module, domainSpecific ? DomibusPropertyMetadataDTO.Usage.DOMAIN : Usage.GLOBAL, false);
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

    /**
     * @deprecated Use instead {@link eu.domibus.ext.domain.DomibusPropertyMetadataDTO#getUsage() }
     */
    @Deprecated
    public boolean isDomainSpecific() {
        return domainSpecific;
    }

    /**
     * @deprecated Use instead {@link eu.domibus.ext.domain.DomibusPropertyMetadataDTO#setUsage(int) }
     */
    @Deprecated
    public void setDomainSpecific(boolean domainSpecific) {
        this.domainSpecific = domainSpecific;
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
}
