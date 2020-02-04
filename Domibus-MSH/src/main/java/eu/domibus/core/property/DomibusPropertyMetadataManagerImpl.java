package eu.domibus.core.property;

import eu.domibus.api.property.DomibusPropertyManager;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.api.property.DomibusPropertyMetadataManager;
import eu.domibus.ext.delegate.services.property.DomibusPropertyManagerDelegate;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.domain.Module;
import eu.domibus.ext.services.DomibusPropertyManagerExt;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DomibusPropertyMetadataManagerImpl implements DomibusPropertyMetadataManager {
    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(DomibusPropertyMetadataManagerImpl.class);

    @Autowired
    ApplicationContext applicationContext;

    private Map<String, DomibusPropertyMetadata> propertyMetadataMap;
    private volatile boolean internalPropertiesLoaded = false;
    private volatile boolean externalPropertiesLoaded = false;
    private final Object propertyMetadataMapLock = new Object();

    private Map<String, DomibusPropertyMetadata> knownProperties = Arrays.stream(new DomibusPropertyMetadata[]{
            //read-only properties
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DEPLOYMENT_CLUSTERED),
            new DomibusPropertyMetadata(DOMIBUS_SECURITY_KEY_PRIVATE_PASSWORD, false, DomibusPropertyMetadata.Usage.DOMAIN, false, true),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATABASE_GENERAL_SCHEMA),
            new DomibusPropertyMetadata(DOMIBUS_DATABASE_SCHEMA, false, DomibusPropertyMetadata.Usage.DOMAIN, false),

            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATASOURCE_XA_PROPERTY_USER),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATASOURCE_XA_PROPERTY_PASSWORD, true),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATASOURCE_XA_PROPERTY_url),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATASOURCE_XA_PROPERTY_URL),

            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATASOURCE_DRIVER_CLASS_NAME),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATASOURCE_URL),

            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATASOURCE_USER),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATASOURCE_PASSWORD, true),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATASOURCE_MAX_LIFETIME),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATASOURCE_MIN_POOL_SIZE),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_DATASOURCE_MAX_POOL_SIZE),

            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_ENTITY_MANAGER_FACTORY_PACKAGES_TO_SCAN),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_ENTITY_MANAGER_FACTORY_JPA_PROPERTY_HIBERNATE_CONNECTION_DRIVER_CLASS),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_ENTITY_MANAGER_FACTORY_JPA_PROPERTY_HIBERNATE_DIALECT),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_ENTITY_MANAGER_FACTORY_JPA_PROPERTY_HIBERNATE_ID_NEW_GENERATOR_MAPPINGS),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_ENTITY_MANAGER_FACTORY_JPA_PROPERTY_HIBERNATE_FORMAT_SQL),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_ENTITY_MANAGER_FACTORY_JPA_PROPERTY_HIBERNATE_TRANSACTION_FACTORY_CLASS),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_ENTITY_MANAGER_FACTORY_JPA_PROPERTY_HIBERNATE_TRANSACTION_JTA_PLATFORM),

            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_ENCRYPTION_ACTIVE, false, DomibusPropertyMetadata.Usage.GLOBAL_AND_DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_ENCRYPTION_PROPERTIES, false, DomibusPropertyMetadata.Usage.GLOBAL_AND_DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_ENCRYPTION_KEY_LOCATION, false, DomibusPropertyMetadata.Usage.GLOBAL_AND_DOMAIN, true),

            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_JMS_QUEUE_PULL),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_JMS_QUEUE_UI_REPLICATION), //move the use=age from xml ?

            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_JMS_QUEUE_ALERT), //move the use=age from xml ?
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_TASK_EXECUTOR_THREAD_COUNT),  //move the use=age from xml ?

            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_ALERT_QUEUE_CONCURRENCY), //move the use=age from xml ?
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(MESSAGE_FACTORY_CLASS), //move the use=age from xml ?
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(COMPRESSION_BLACKLIST),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_INTERNAL_QUEUE_CONCURENCY), //move the use=age from xml ?

            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_METRICS_JMX_REPORTER_ENABLE),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_METRICS_SL_4_J_REPORTER_ENABLE),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_METRICS_SL_4_J_REPORTER_PERIOD_TIME_UNIT),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_METRICS_SL_4_J_REPORTER_PERIOD_NUMBER),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_METRICS_MONITOR_MEMORY),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_METRICS_MONITOR_GC),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_METRICS_MONITOR_CACHED_THREADS),
            DomibusPropertyMetadata.getReadOnlyGlobalProperty(DOMIBUS_METRICS_MONITOR_JMS_QUEUES),

            DomibusPropertyMetadata.getReadOnlyGlobalProperty(WEBLOGIC_MANAGEMENT_SERVER),

            new DomibusPropertyMetadata(DOMIBUS_PULL_REQUEST_SEND_PER_JOB_CYCLE_PER_MPC, Module.MSH, false, DomibusPropertyMetadata.Usage.DOMAIN, true, true, false, true),

            //writable properties
            new DomibusPropertyMetadata(DOMIBUS_UI_TITLE_NAME, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_UI_REPLICATION_ENABLED, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_UI_SUPPORT_TEAM_NAME, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_UI_SUPPORT_TEAM_EMAIL, DomibusPropertyMetadata.Usage.DOMAIN, true),

            new DomibusPropertyMetadata(DOMIBUS_SECURITY_KEYSTORE_LOCATION, DomibusPropertyMetadata.Usage.DOMAIN, false),
            new DomibusPropertyMetadata(DOMIBUS_SECURITY_KEYSTORE_TYPE, DomibusPropertyMetadata.Usage.DOMAIN, false),
            new DomibusPropertyMetadata(DOMIBUS_SECURITY_KEYSTORE_PASSWORD, true, DomibusPropertyMetadata.Usage.DOMAIN, false, true),
            new DomibusPropertyMetadata(DOMIBUS_SECURITY_KEY_PRIVATE_ALIAS, DomibusPropertyMetadata.Usage.DOMAIN, false),

            new DomibusPropertyMetadata(DOMIBUS_SECURITY_TRUSTSTORE_LOCATION, DomibusPropertyMetadata.Usage.DOMAIN, false),
            new DomibusPropertyMetadata(DOMIBUS_SECURITY_TRUSTSTORE_TYPE, DomibusPropertyMetadata.Usage.DOMAIN, false),
            new DomibusPropertyMetadata(DOMIBUS_SECURITY_TRUSTSTORE_PASSWORD, true, DomibusPropertyMetadata.Usage.DOMAIN, false, true),

            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_AUTH_UNSECURE_LOGIN_ALLOWED),
            new DomibusPropertyMetadata(DOMIBUS_CONSOLE_LOGIN_MAXIMUM_ATTEMPT, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_CONSOLE_LOGIN_SUSPENSION_TIME, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_CERTIFICATE_REVOCATION_OFFSET, DomibusPropertyMetadata.Usage.DOMAIN, false),
            new DomibusPropertyMetadata(DOMIBUS_CERTIFICATE_CRL_EXCLUDED_PROTOCOLS, DomibusPropertyMetadata.Usage.DOMAIN, false),

            new DomibusPropertyMetadata(DOMIBUS_PLUGIN_LOGIN_MAXIMUM_ATTEMPT, DomibusPropertyMetadata.Usage.DOMAIN, false),
            new DomibusPropertyMetadata(DOMIBUS_PLUGIN_LOGIN_SUSPENSION_TIME, DomibusPropertyMetadata.Usage.DOMAIN, true),

            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_PATTERN, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_VALIDATION_MESSAGE, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_EXPIRATION, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_DEFAULT_PASSWORD_EXPIRATION, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_WARNING_BEFORE_EXPIRATION, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_DONT_REUSE_LAST, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_PASSWORD_POLICY_CHECK_DEFAULT_PASSWORD),

            new DomibusPropertyMetadata(DOMIBUS_PLUGIN_PASSWORD_POLICY_PATTERN, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PLUGIN_PASSWORD_POLICY_VALIDATION_MESSAGE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_PLUGIN_EXPIRATION, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_PLUGIN_DEFAULT_PASSWORD_EXPIRATION, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_PLUGIN_DONT_REUSE_LAST, DomibusPropertyMetadata.Usage.DOMAIN, true),

            new DomibusPropertyMetadata(DOMIBUS_ATTACHMENT_TEMP_STORAGE_LOCATION, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ATTACHMENT_STORAGE_LOCATION, DomibusPropertyMetadata.Usage.DOMAIN, false),
            new DomibusPropertyMetadata(DOMIBUS_PAYLOAD_ENCRYPTION_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN, true),

            new DomibusPropertyMetadata(DOMIBUS_MSH_MESSAGEID_SUFFIX, DomibusPropertyMetadata.Usage.DOMAIN, true),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_MSH_RETRY_MESSAGE_EXPIRATION_DELAY),

            new DomibusPropertyMetadata(DOMIBUS_DYNAMICDISCOVERY_USE_DYNAMIC_DISCOVERY, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_SMLZONE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DYNAMICDISCOVERY_CLIENT_SPECIFICATION, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DYNAMICDISCOVERY_PEPPOLCLIENT_MODE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DYNAMICDISCOVERY_OASISCLIENT_REGEX_CERTIFICATE_SUBJECT_VALIDATION, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DYNAMICDISCOVERY_PARTYID_RESPONDER_ROLE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DYNAMICDISCOVERY_PARTYID_TYPE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DYNAMICDISCOVERY_TRANSPORTPROFILEAS_4, DomibusPropertyMetadata.Usage.DOMAIN, true),

            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_LIST_PENDING_MESSAGES_MAX_COUNT),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_JMS_QUEUE_MAX_BROWSE_SIZE), //there is one place at init time that it is not refreshed
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_JMS_INTERNAL_QUEUE_EXPRESSION),

            new DomibusPropertyMetadata(DOMIBUS_RECEIVER_CERTIFICATE_VALIDATION_ONSENDING, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONSENDING, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION, DomibusPropertyMetadata.Usage.DOMAIN, false),
            new DomibusPropertyMetadata(DOMIBUS_SENDER_CERTIFICATE_SUBJECT_CHECK, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_SENDER_TRUST_VALIDATION_TRUSTSTORE_ALIAS, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN, DomibusPropertyMetadata.Usage.DOMAIN, false),

            new DomibusPropertyMetadata(DOMIBUS_DISPATCHER_CONNECTION_TIMEOUT, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DISPATCHER_RECEIVE_TIMEOUT, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DISPATCHER_ALLOW_CHUNKING, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DISPATCHER_CHUNKING_THRESHOLD, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DISPATCHER_CONCURENCY, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DISPATCHER_LARGE_FILES_CONCURRENCY, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DISPATCHER_CACHEABLE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DISPATCHER_CONNECTION_KEEP_ALIVE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_RETENTION_WORKER_MESSAGE_RETENTION_DOWNLOADED_MAX_DELETE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_RETENTION_WORKER_MESSAGE_RETENTION_NOT_DOWNLOADED_MAX_DELETE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_RETENTION_JMS_CONCURRENCY, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DISPATCH_EBMS_ERROR_UNRECOVERABLE_RETRY, DomibusPropertyMetadata.Usage.DOMAIN, true),

            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_PROXY_ENABLED),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_PROXY_HTTP_HOST),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_PROXY_HTTP_PORT),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_PROXY_USER),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_PROXY_PASSWORD),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_PROXY_NON_PROXY_HOSTS),

            new DomibusPropertyMetadata(DOMIBUS_UI_REPLICATION_SYNC_CRON_MAX_ROWS, DomibusPropertyMetadata.Usage.DOMAIN, true), //there is still one call from xml!!!
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_PLUGIN_NOTIFICATION_ACTIVE),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_NONREPUDIATION_AUDIT_ACTIVE),
            new DomibusPropertyMetadata(DOMIBUS_SEND_MESSAGE_FAILURE_DELETE_PAYLOAD, DomibusPropertyMetadata.Usage.DOMAIN, true),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_SEND_MESSAGE_ATTEMPT_AUDIT_ACTIVE),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_FOURCORNERMODEL_ENABLED),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_LOGGING_PAYLOAD_PRINT),     //there are still usages in xml!!!! move them?
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_LOGGING_CXF_LIMIT),         //there are still usages in xml!!!! move them?

            new DomibusPropertyMetadata(DOMIBUS_PAYLOAD_TEMP_JOB_RETENTION_EXCLUDE_REGEX, DomibusPropertyMetadata.Usage.DOMAIN, false),
            new DomibusPropertyMetadata(DOMIBUS_PAYLOAD_TEMP_JOB_RETENTION_EXPIRATION, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PAYLOAD_TEMP_JOB_RETENTION_DIRECTORIES, DomibusPropertyMetadata.Usage.DOMAIN, false),

            new DomibusPropertyMetadata(DOMIBUS_DISPATCHER_SPLIT_AND_JOIN_CONCURRENCY, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_DISPATCHER_SPLIT_AND_JOIN_PAYLOADS_SCHEDULE_THRESHOLD, DomibusPropertyMetadata.Usage.DOMAIN, true),

            new DomibusPropertyMetadata(DOMAIN_TITLE, DomibusPropertyMetadata.Usage.DOMAIN, false),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_USER_INPUT_BLACK_LIST),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_USER_INPUT_WHITE_LIST),

            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_ACCOUNT_UNLOCK_CRON),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_CERTIFICATE_CHECK_CRON),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_PLUGIN_ACCOUNT_UNLOCK_CRON),
            new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICIES_CHECK_CRON, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_PLUGIN_PASSWORD_POLICIES_CHECK_CRON, DomibusPropertyMetadata.Usage.DOMAIN, true),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_PAYLOAD_TEMP_JOB_RETENTION_CRON),
            new DomibusPropertyMetadata(DOMIBUS_MSH_RETRY_CRON, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_RETENTION_WORKER_CRON_EXPRESSION, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_MSH_PULL_CRON, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PULL_RETRY_CRON, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CLEANER_CRON, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_RETRY_CRON, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_UI_REPLICATION_SYNC_CRON),
            new DomibusPropertyMetadata(DOMIBUS_SPLIT_AND_JOIN_RECEIVE_EXPIRATION_CRON, DomibusPropertyMetadata.Usage.DOMAIN, true),

            new DomibusPropertyMetadata(DOMIBUS_ALERT_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_MAIL_SENDING_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_ALERT_MAIL_SMTP_TIMEOUT),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_ALERT_SENDER_SMTP_URL),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_ALERT_SENDER_SMTP_PORT),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_ALERT_SENDER_SMTP_USER),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_ALERT_SENDER_SMTP_PASSWORD),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_SENDER_EMAIL, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, false),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_RECEIVER_EMAIL, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, false),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CLEANER_ALERT_LIFETIME, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_RETRY_TIME, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_RETRY_MAX_ATTEMPTS, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_STATES, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_LEVEL, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_MAIL_SUBJECT, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_USER_LOGIN_FAILURE_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_USER_LOGIN_FAILURE_LEVEL, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_USER_LOGIN_FAILURE_MAIL_SUBJECT, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_USER_ACCOUNT_DISABLED_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_USER_ACCOUNT_DISABLED_LEVEL, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_USER_ACCOUNT_DISABLED_MOMENT, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_USER_ACCOUNT_DISABLED_SUBJECT, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_DELAY_DAYS, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_FREQUENCY_DAYS, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_LEVEL, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_MAIL_SUBJECT, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CERT_EXPIRED_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CERT_EXPIRED_FREQUENCY_DAYS, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CERT_EXPIRED_DURATION_DAYS, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CERT_EXPIRED_LEVEL, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_CERT_EXPIRED_MAIL_SUBJECT, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PASSWORD_IMMINENT_EXPIRATION_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PASSWORD_IMMINENT_EXPIRATION_DELAY_DAYS, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PASSWORD_IMMINENT_EXPIRATION_FREQUENCY_DAYS, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PASSWORD_IMMINENT_EXPIRATION_LEVEL, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PASSWORD_IMMINENT_EXPIRATION_MAIL_SUBJECT, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PASSWORD_EXPIRED_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PASSWORD_EXPIRED_DELAY_DAYS, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PASSWORD_EXPIRED_FREQUENCY_DAYS, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PASSWORD_EXPIRED_LEVEL, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PASSWORD_EXPIRED_MAIL_SUBJECT, DomibusPropertyMetadata.Usage.DOMAIN_AND_SUPER, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_PASSWORD_IMMINENT_EXPIRATION_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_PASSWORD_IMMINENT_EXPIRATION_DELAY_DAYS, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_PASSWORD_IMMINENT_EXPIRATION_FREQUENCY_DAYS, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_PASSWORD_IMMINENT_EXPIRATION_LEVEL, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_PASSWORD_IMMINENT_EXPIRATION_MAIL_SUBJECT, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_PASSWORD_EXPIRED_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_PASSWORD_EXPIRED_DELAY_DAYS, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_PASSWORD_EXPIRED_FREQUENCY_DAYS, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_PASSWORD_EXPIRED_LEVEL, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_PASSWORD_EXPIRED_MAIL_SUBJECT, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_USER_LOGIN_FAILURE_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_USER_LOGIN_FAILURE_LEVEL, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_USER_LOGIN_FAILURE_MAIL_SUBJECT, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_USER_ACCOUNT_DISABLED_ACTIVE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_USER_ACCOUNT_DISABLED_LEVEL, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_USER_ACCOUNT_DISABLED_MOMENT, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_ALERT_PLUGIN_USER_ACCOUNT_DISABLED_SUBJECT, DomibusPropertyMetadata.Usage.DOMAIN, true),

            new DomibusPropertyMetadata(DOMIBUS_PULL_REQUEST_SEND_PER_JOB_CYCLE, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PULL_REQUEST_FREQUENCY_RECOVERY_TIME, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PULL_REQUEST_FREQUENCY_ERROR_COUNT, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PULL_DYNAMIC_INITIATOR, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PULL_MULTIPLE_LEGS, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PULL_FORCE_BY_MPC, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PULL_MPC_INITIATOR_SEPARATOR, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PULL_RECEIPT_QUEUE_CONCURRENCY, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_PULL_QUEUE_CONCURENCY, DomibusPropertyMetadata.Usage.DOMAIN, true),

            new DomibusPropertyMetadata(DOMIBUS_EXTENSION_IAM_AUTHENTICATION_IDENTIFIER, DomibusPropertyMetadata.Usage.DOMAIN, true),
            new DomibusPropertyMetadata(DOMIBUS_EXTENSION_IAM_AUTHORIZATION_IDENTIFIER, DomibusPropertyMetadata.Usage.DOMAIN, true),

            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_EXCEPTIONS_REST_ENABLE),
            DomibusPropertyMetadata.getGlobalProperty(DOMIBUS_INSTANCE_NAME),
    }).collect(Collectors.toMap(x -> x.getName(), x -> x));

    /**
     * Returns all the properties that this PropertyProvider is able to handle, writable and read-only alike.
     *
     * @return a map
     * @implNote This list will be moved in the database eventually.
     */
    @Override
    public Map<String, DomibusPropertyMetadata> getKnownProperties() {
        return knownProperties;
    }

    @Override
    public boolean hasKnownProperty(String name) {
        return this.getKnownProperties().containsKey(name);
    }

    /**
     * Returns the metadata for a given propertyName,
     * by interrogating all property managers known to Domibus in order to find it.
     * If not found, it assumes it is a global property and it creates the corresponding metadata on-the-fly.
     *
     * @param propertyName
     * @return DomibusPropertyMetadata
     */
    public DomibusPropertyMetadata getPropertyMetadata(String propertyName) {
        initializeIfNeeded(propertyName);

        DomibusPropertyMetadata prop = propertyMetadataMap.get(propertyName);
        if (prop != null) {
            LOGGER.trace("Found property [{}], returning its metadata.", propertyName);
            return prop;
        }

        // try to see if it is a compose-able property, i.e. propertyName+suffix
        Optional<DomibusPropertyMetadata> propMeta = propertyMetadataMap.values().stream().filter(p -> p.isComposable() && propertyName.startsWith(p.getName())).findAny();
        if (propMeta.isPresent()) {
            LOGGER.trace("Found compose-able property [{}], returning its metadata.", propertyName);
            DomibusPropertyMetadata meta = propMeta.get();
            // metadata name is a prefix of propertyName so we set the whole property name here to be correctly used down the stream. Not beautiful
            meta.setName(propertyName);
            return meta;
        }

        // if still not found, initialize metadata on-the-fly
        LOGGER.warn("Creating on-the-fly global metadata for unknown property: [{}]", propertyName); //TODO: lower log level after testing
        synchronized (propertyMetadataMapLock) {
            DomibusPropertyMetadata newProp = DomibusPropertyMetadata.getReadOnlyGlobalProperty(propertyName, null);
            propertyMetadataMap.put(propertyName, newProp);
            return newProp;
        }
    }

    /**
     * Initializes the metadata map.
     * Initially, during the bean creation stage, only a few domibus-core properties are needed;
     * later on, the properties from all managers will be added to the map.
     */
    protected void initializeIfNeeded(String propertyName) {
        // add domibus-core and specific server  properties first, to avoid infinite loop of bean creation (due to DB properties)
        if (propertyMetadataMap == null) {
            synchronized (propertyMetadataMapLock) {
                if (!internalPropertiesLoaded) { // double-check locking
                    LOGGER.trace("Initializing core properties");

                    propertyMetadataMap = new HashMap<>();
                    loadInternalProperties();

                    LOGGER.trace("Finished loading property metadata for internal property managers.");
                    internalPropertiesLoaded = true;
                }
            }
        }
        if (propertyMetadataMap.containsKey(propertyName)) {
            LOGGER.trace("Found property metadata [{}] in core properties. Returning.", propertyName);
            return;
        }

        // load external properties (i.e. plugin properties and extension properties) the first time one of them is needed
        if (!externalPropertiesLoaded) {
            synchronized (propertyMetadataMapLock) {
                if (!externalPropertiesLoaded) { // double-check locking
                    LOGGER.trace("Initializing external properties");

                    loadExternalProperties();

                    externalPropertiesLoaded = true;
                    LOGGER.trace("Finished loading property metadata for external property managers.");
                }
            }
        }
    }

    protected void loadInternalProperties() {
        // load manually core/msh/common 'own' properties to avoid  infinite loop
        loadProperties(this, DomibusPropertyManager.MSH_PROPERTY_MANAGER);

        // server specific properties (and maybe others in the future)
        String[] propertyManagerNames = applicationContext.getBeanNamesForType(DomibusPropertyManager.class);
        Arrays.asList(propertyManagerNames).stream()
                //exclude me/this one
                .filter(el -> !el.equals(DomibusPropertyManager.MSH_PROPERTY_MANAGER))
                .forEach(managerName -> {
                    DomibusPropertyManager propertyManager = applicationContext.getBean(managerName, DomibusPropertyManager.class);
                    loadProperties(propertyManager, managerName);
                });
    }

    protected void loadProperties(DomibusPropertyMetadataManager propertyManager, String managerName) {
        LOGGER.trace("Loading property metadata for [{}] property manager.", managerName);
        for (Map.Entry<String, DomibusPropertyMetadata> entry : propertyManager.getKnownProperties().entrySet()) {
            DomibusPropertyMetadata prop = entry.getValue();
            propertyMetadataMap.put(entry.getKey(), prop);
        }
    }

    protected void loadExternalProperties() {
        // we retrieve here all managers: one for each plugin and extension
        Map<String, DomibusPropertyManagerExt> propertyManagers = applicationContext.getBeansOfType(DomibusPropertyManagerExt.class);
        // We get also domibus property manager delegate (which adapts DomibusPropertyManager to DomibusPropertyManagerExt) which is already loaded so we remove it first
        propertyManagers.remove(DomibusPropertyManagerDelegate.MSH_DELEGATE);
        propertyManagers.entrySet().forEach(this::loadExternalProperties);
    }

    protected void loadExternalProperties(Map.Entry<String, DomibusPropertyManagerExt> mapEntry) {
        DomibusPropertyManagerExt propertyManager = mapEntry.getValue();
        LOGGER.trace("Loading property metadata for [{}] external property manager.", mapEntry.getKey());
        for (Map.Entry<String, DomibusPropertyMetadataDTO> entry : propertyManager.getKnownProperties().entrySet()) {
            DomibusPropertyMetadataDTO extProp = entry.getValue();
            DomibusPropertyMetadata domibusProp = new DomibusPropertyMetadata(extProp.getName(), extProp.getModule(), extProp.isWritable(), extProp.getUsage(), extProp.isWithFallback(),
                    extProp.isClusterAware(), extProp.isEncrypted(), extProp.isComposable());
            propertyMetadataMap.put(entry.getKey(), domibusProp);
        }
    }

}
