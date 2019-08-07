package eu.domibus.api.property;

import java.util.Map;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * <p>
 * The interface implemented by MSH and the plugins to handle getting and setting of domibus properties at runtime
 */
public interface DomibusPropertyMetadataManager {

    String DOMIBUS_UI_TITLE_NAME = "domibus.UI.title.name";
    String DOMIBUS_UI_REPLICATION_ENABLED = "domibus.ui.replication.enabled";
    String DOMIBUS_UI_SUPPORT_TEAM_NAME = "domibus.ui.support.team.name";
    String DOMIBUS_UI_SUPPORT_TEAM_EMAIL = "domibus.ui.support.team.email";
    String DOMIBUS_SECURITY_KEYSTORE_LOCATION = "domibus.security.keystore.location";
    String DOMIBUS_SECURITY_KEYSTORE_TYPE = "domibus.security.keystore.type";
    String DOMIBUS_SECURITY_KEYSTORE_PASSWORD = "domibus.security.keystore.password";
    String DOMIBUS_SECURITY_KEY_PRIVATE_ALIAS = "domibus.security.key.private.alias";
    String DOMIBUS_SECURITY_TRUSTSTORE_LOCATION = "domibus.security.truststore.location";
    String DOMIBUS_SECURITY_TRUSTSTORE_TYPE = "domibus.security.truststore.type";
    String DOMIBUS_SECURITY_TRUSTSTORE_PASSWORD = "domibus.security.truststore.password";
    String DOMIBUS_AUTH_UNSECURE_LOGIN_ALLOWED = "domibus.auth.unsecureLoginAllowed";
    String DOMIBUS_CONSOLE_LOGIN_MAXIMUM_ATTEMPT = "domibus.console.login.maximum.attempt";
    String DOMIBUS_CONSOLE_LOGIN_SUSPENSION_TIME = "domibus.console.login.suspension.time";
    String DOMIBUS_CERTIFICATE_REVOCATION_OFFSET = "domibus.certificate.revocation.offset";
    String DOMIBUS_CERTIFICATE_CRL_EXCLUDED_PROTOCOLS = "domibus.certificate.crl.excludedProtocols";
    String DOMIBUS_PLUGIN_LOGIN_MAXIMUM_ATTEMPT = "domibus.plugin.login.maximum.attempt";
    String DOMIBUS_PLUGIN_LOGIN_SUSPENSION_TIME = "domibus.plugin.login.suspension.time";
    String DOMIBUS_PASSWORD_POLICY_PATTERN = "domibus.passwordPolicy.pattern";
    String DOMIBUS_PASSWORD_POLICY_VALIDATION_MESSAGE = "domibus.passwordPolicy.validationMessage";
    String DOMIBUS_PASSWORD_POLICY_EXPIRATION = "domibus.passwordPolicy.expiration";
    String DOMIBUS_PASSWORD_POLICY_DEFAULT_PASSWORD_EXPIRATION = "domibus.passwordPolicy.defaultPasswordExpiration";
    String DOMIBUS_PASSWORD_POLICY_WARNING_BEFORE_EXPIRATION = "domibus.passwordPolicy.warning.beforeExpiration";
    String DOMIBUS_PASSWORD_POLICY_DONT_REUSE_LAST = "domibus.passwordPolicy.dontReuseLast";
    String DOMIBUS_PASSWORD_POLICY_CHECK_DEFAULT_PASSWORD = "domibus.passwordPolicy.checkDefaultPassword";
    String DOMIBUS_PLUGIN_PASSWORD_POLICY_PATTERN = "domibus.plugin.passwordPolicy.pattern";
    String DOMIBUS_PLUGIN_PASSWORD_POLICY_VALIDATION_MESSAGE = "domibus.plugin.passwordPolicy.validationMessage";
    String DOMIBUS_PASSWORD_POLICY_PLUGIN_EXPIRATION = "domibus.plugin.passwordPolicy.expiration";
    String DOMIBUS_PASSWORD_POLICY_PLUGIN_DEFAULT_PASSWORD_EXPIRATION = "domibus.plugin.passwordPolicy.defaultPasswordExpiration";
    String DOMIBUS_PASSWORD_POLICY_PLUGIN_DONT_REUSE_LAST = "domibus.plugin.passwordPolicy.dontReuseLast";
    String DOMIBUS_ATTACHMENT_STORAGE_LOCATION = "domibus.attachment.storage.location";
    String DOMIBUS_PAYLOAD_ENCRYPTION_ACTIVE = "domibus.payload.encryption.active";
    String DOMIBUS_MSH_MESSAGEID_SUFFIX = "domibus.msh.messageid.suffix";
    String DOMIBUS_MSH_RETRY_MESSAGE_EXPIRATION_DELAY = "domibus.msh.retry.messageExpirationDelay";
    String DOMIBUS_DYNAMICDISCOVERY_USE_DYNAMIC_DISCOVERY = "domibus.dynamicdiscovery.useDynamicDiscovery";
    String DOMIBUS_SMLZONE = "domibus.smlzone";
    String DOMIBUS_DYNAMICDISCOVERY_CLIENT_SPECIFICATION = "domibus.dynamicdiscovery.client.specification";
    String DOMIBUS_DYNAMICDISCOVERY_PEPPOLCLIENT_MODE = "domibus.dynamicdiscovery.peppolclient.mode";
    String DOMIBUS_DYNAMICDISCOVERY_OASISCLIENT_REGEX_CERTIFICATE_SUBJECT_VALIDATION = "domibus.dynamicdiscovery.oasisclient.regexCertificateSubjectValidation";
    String DOMIBUS_DYNAMICDISCOVERY_PARTYID_RESPONDER_ROLE = "domibus.dynamicdiscovery.partyid.responder.role";
    String DOMIBUS_DYNAMICDISCOVERY_PARTYID_TYPE = "domibus.dynamicdiscovery.partyid.type";
    String DOMIBUS_DYNAMICDISCOVERY_TRANSPORTPROFILEAS_4 = "domibus.dynamicdiscovery.transportprofileas4";
    String DOMIBUS_LIST_PENDING_MESSAGES_MAX_COUNT = "domibus.listPendingMessages.maxCount";
    String DOMIBUS_JMS_QUEUE_MAX_BROWSE_SIZE = "domibus.jms.queue.maxBrowseSize";
    String DOMIBUS_JMS_INTERNAL_QUEUE_EXPRESSION = "domibus.jms.internalQueue.expression";
    String DOMIBUS_RECEIVER_CERTIFICATE_VALIDATION_ONSENDING = "domibus.receiver.certificate.validation.onsending";
    String DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONSENDING = "domibus.sender.certificate.validation.onsending";
    String DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING = "domibus.sender.certificate.validation.onreceiving";
    String DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING = "domibus.sender.trust.validation.onreceiving";
    String DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION = "domibus.sender.trust.validation.expression";
    String DOMIBUS_SENDER_CERTIFICATE_SUBJECT_CHECK = "domibus.sender.certificate.subject.check";
    String DOMIBUS_SENDER_TRUST_VALIDATION_TRUSTSTORE_ALIAS = "domibus.sender.trust.validation.truststore_alias";
    String DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN = "domibus.sendMessage.messageIdPattern";
    String DOMIBUS_DISPATCHER_CONNECTION_TIMEOUT = "domibus.dispatcher.connectionTimeout";
    String DOMIBUS_DISPATCHER_RECEIVE_TIMEOUT = "domibus.dispatcher.receiveTimeout";
    String DOMIBUS_DISPATCHER_ALLOW_CHUNKING = "domibus.dispatcher.allowChunking";
    String DOMIBUS_DISPATCHER_CHUNKING_THRESHOLD = "domibus.dispatcher.chunkingThreshold";
    String DOMIBUS_DISPATCHER_CONCURENCY = "domibus.dispatcher.concurency";
    String DOMIBUS_DISPATCHER_LARGE_FILES_CONCURRENCY = "domibus.dispatcher.largeFiles.concurrency";
    String DOMIBUS_DISPATCHER_CACHEABLE = "domibus.dispatcher.cacheable";
    String DOMIBUS_DISPATCHER_CONNECTION_KEEP_ALIVE = "domibus.dispatcher.connection.keepAlive";
    String DOMIBUS_RETENTION_WORKER_MESSAGE_RETENTION_DOWNLOADED_MAX_DELETE = "domibus.retentionWorker.message.retention.downloaded.max.delete";
    String DOMIBUS_RETENTION_WORKER_MESSAGE_RETENTION_NOT_DOWNLOADED_MAX_DELETE = "domibus.retentionWorker.message.retention.not_downloaded.max.delete";
    String DOMIBUS_RETENTION_JMS_CONCURRENCY = "domibus.retention.jms.concurrency";
    String DOMIBUS_DISPATCH_EBMS_ERROR_UNRECOVERABLE_RETRY = "domibus.dispatch.ebms.error.unrecoverable.retry";
    String DOMIBUS_PROXY_ENABLED = "domibus.proxy.enabled";
    String DOMIBUS_PROXY_HTTP_HOST = "domibus.proxy.http.host";
    String DOMIBUS_PROXY_HTTP_PORT = "domibus.proxy.http.port";
    String DOMIBUS_PROXY_USER = "domibus.proxy.user";
    String DOMIBUS_PROXY_PASSWORD = "domibus.proxy.password"; //NOSONAR: This is not a hardcoded password, it is just the name of a property
    String DOMIBUS_PROXY_NON_PROXY_HOSTS = "domibus.proxy.nonProxyHosts";
    String DOMIBUS_UI_REPLICATION_SYNC_CRON_MAX_ROWS = "domibus.ui.replication.sync.cron.max.rows";
    String DOMIBUS_PLUGIN_NOTIFICATION_ACTIVE = "domibus.plugin.notification.active";
    String DOMIBUS_NONREPUDIATION_AUDIT_ACTIVE = "domibus.nonrepudiation.audit.active";
    String DOMIBUS_SEND_MESSAGE_FAILURE_DELETE_PAYLOAD = "domibus.sendMessage.failure.delete.payload";
    String DOMIBUS_SEND_MESSAGE_ATTEMPT_AUDIT_ACTIVE = "domibus.sendMessage.attempt.audit.active";
    String DOMIBUS_FOURCORNERMODEL_ENABLED = "domibus.fourcornermodel.enabled";
    String DOMIBUS_LOGGING_PAYLOAD_PRINT = "domibus.logging.payload.print";
    String DOMIBUS_ATTACHMENT_TEMP_STORAGE_LOCATION = "domibus.attachment.temp.storage.location";
    String DOMIBUS_DISPATCHER_SPLIT_AND_JOIN_CONCURRENCY = "domibus.dispatcher.splitAndJoin.concurrency";
    String DOMIBUS_DISPATCHER_SPLIT_AND_JOIN_PAYLOADS_SCHEDULE_THRESHOLD = "domibus.dispatcher.splitAndJoin.payloads.schedule.threshold";
    String DOMAIN_TITLE = "domain.title";
    String DOMIBUS_USER_INPUT_BLACK_LIST = "domibus.userInput.blackList";
    String DOMIBUS_USER_INPUT_WHITE_LIST = "domibus.userInput.whiteList";
    String DOMIBUS_ACCOUNT_UNLOCK_CRON = "domibus.account.unlock.cron";
    String DOMIBUS_CERTIFICATE_CHECK_CRON = "domibus.certificate.check.cron";
    String DOMIBUS_PLUGIN_ACCOUNT_UNLOCK_CRON = "domibus.plugin.account.unlock.cron";
    String DOMIBUS_PASSWORD_POLICIES_CHECK_CRON = "domibus.passwordPolicies.check.cron";
    String DOMIBUS_PLUGIN_PASSWORD_POLICIES_CHECK_CRON = "domibus.plugin_passwordPolicies.check.cron";
    String DOMIBUS_PAYLOAD_TEMP_JOB_RETENTION_CRON = "domibus.payload.temp.job.retention.cron";
    String DOMIBUS_MSH_RETRY_CRON = "domibus.msh.retry.cron";
    String DOMIBUS_RETENTION_WORKER_CRON_EXPRESSION = "domibus.retentionWorker.cronExpression";
    String DOMIBUS_MSH_PULL_CRON = "domibus.msh.pull.cron";
    String DOMIBUS_PULL_RETRY_CRON = "domibus.pull.retry.cron";
    String DOMIBUS_ALERT_CLEANER_CRON = "domibus.alert.cleaner.cron";
    String DOMIBUS_ALERT_RETRY_CRON = "domibus.alert.retry.cron";
    String DOMIBUS_ALERT_SUPER_CLEANER_CRON = "domibus.alert.super.cleaner.cron";
    String DOMIBUS_ALERT_SUPER_RETRY_CRON = "domibus.alert.super.retry.cron";
    String DOMIBUS_UI_REPLICATION_SYNC_CRON = "domibus.ui.replication.sync.cron";
    String DOMIBUS_SPLIT_AND_JOIN_RECEIVE_EXPIRATION_CRON = "domibus.splitAndJoin.receive.expiration.cron";

    String DOMIBUS_ALERT_ACTIVE = "domibus.alert.active";
    String DOMIBUS_ALERT_MAIL_SENDING_ACTIVE = "domibus.alert.mail.sending.active";
    String DOMIBUS_ALERT_SENDER_SMTP_URL = "domibus.alert.sender.smtp.url";
    String DOMIBUS_ALERT_SENDER_SMTP_PORT = "domibus.alert.sender.smtp.port";
    String DOMIBUS_ALERT_SENDER_SMTP_USER = "domibus.alert.sender.smtp.user";
    String DOMIBUS_ALERT_SENDER_SMTP_PASSWORD = "domibus.alert.sender.smtp.password";
    String DOMIBUS_ALERT_SENDER_EMAIL = "domibus.alert.sender.email";
    String DOMIBUS_ALERT_RECEIVER_EMAIL = "domibus.alert.receiver.email";
    String DOMIBUS_ALERT_CLEANER_ALERT_LIFETIME = "domibus.alert.cleaner.alert.lifetime";
    String DOMIBUS_ALERT_RETRY_TIME = "domibus.alert.retry.time";
    String DOMIBUS_ALERT_RETRY_MAX_ATTEMPTS = "domibus.alert.retry.max_attempts";
    String DOMIBUS_ALERT_SUPER_CLEANER_ALERT_LIFETIME = "domibus.alert.super.cleaner.alert.lifetime";
    String DOMIBUS_ALERT_SUPER_ACTIVE = "domibus.alert.super.active";
    String DOMIBUS_ALERT_SUPER_MAIL_SENDING_ACTIVE = "domibus.alert.super.mail.sending.active";
    String DOMIBUS_ALERT_SUPER_RETRY_TIME = "domibus.alert.super.retry.time";
    String DOMIBUS_ALERT_SUPER_RETRY_MAX_ATTEMPTS = "domibus.alert.super.retry.max_attempts";
    String DOMIBUS_ALERT_SUPER_SENDER_EMAIL = "domibus.alert.super.sender.email";
    String DOMIBUS_ALERT_SUPER_RECEIVER_EMAIL = "domibus.alert.super.receiver.email";
    String DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_ACTIVE = "domibus.alert.msg.communication_failure.active";
    String DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_STATES = "domibus.alert.msg.communication_failure.states";
    String DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_LEVEL = "domibus.alert.msg.communication_failure.level";
    String DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_MAIL_SUBJECT = "domibus.alert.msg.communication_failure.mail.subject";
    String DOMIBUS_ALERT_USER_LOGIN_FAILURE_ACTIVE = "domibus.alert.user.login_failure.active";
    String DOMIBUS_ALERT_USER_LOGIN_FAILURE_LEVEL = "domibus.alert.user.login_failure.level";
    String DOMIBUS_ALERT_USER_LOGIN_FAILURE_MAIL_SUBJECT = "domibus.alert.user.login_failure.mail.subject";
    String DOMIBUS_ALERT_USER_ACCOUNT_DISABLED_ACTIVE = "domibus.alert.user.account_disabled.active";
    String DOMIBUS_ALERT_USER_ACCOUNT_DISABLED_LEVEL = "domibus.alert.user.account_disabled.level";
    String DOMIBUS_ALERT_USER_ACCOUNT_DISABLED_MOMENT = "domibus.alert.user.account_disabled.moment";
    String DOMIBUS_ALERT_USER_ACCOUNT_DISABLED_SUBJECT = "domibus.alert.user.account_disabled.subject";
    String DOMIBUS_ALERT_SUPER_USER_LOGIN_FAILURE_ACTIVE = "domibus.alert.super.user.login_failure.active";
    String DOMIBUS_ALERT_SUPER_USER_LOGIN_FAILURE_LEVEL = "domibus.alert.super.user.login_failure.level";
    String DOMIBUS_ALERT_SUPER_USER_LOGIN_FAILURE_MAIL_SUBJECT = "domibus.alert.super.user.login_failure.mail.subject";
    String DOMIBUS_ALERT_SUPER_USER_ACCOUNT_DISABLED_ACTIVE = "domibus.alert.super.user.account_disabled.active";
    String DOMIBUS_ALERT_SUPER_USER_ACCOUNT_DISABLED_LEVEL = "domibus.alert.super.user.account_disabled.level";
    String DOMIBUS_ALERT_SUPER_USER_ACCOUNT_DISABLED_MOMENT = "domibus.alert.super.user.account_disabled.moment";
    String DOMIBUS_ALERT_SUPER_USER_ACCOUNT_DISABLED_SUBJECT = "domibus.alert.super.user.account_disabled.subject";
    String DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_ACTIVE = "domibus.alert.cert.imminent_expiration.active";
    String DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_DELAY_DAYS = "domibus.alert.cert.imminent_expiration.delay_days";
    String DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_FREQUENCY_DAYS = "domibus.alert.cert.imminent_expiration.frequency_days";
    String DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_LEVEL = "domibus.alert.cert.imminent_expiration.level";
    String DOMIBUS_ALERT_CERT_IMMINENT_EXPIRATION_MAIL_SUBJECT = "domibus.alert.cert.imminent_expiration.mail.subject";
    String DOMIBUS_ALERT_CERT_EXPIRED_ACTIVE = "domibus.alert.cert.expired.active";
    String DOMIBUS_ALERT_CERT_EXPIRED_FREQUENCY_DAYS = "domibus.alert.cert.expired.frequency_days";
    String DOMIBUS_ALERT_CERT_EXPIRED_DURATION_DAYS = "domibus.alert.cert.expired.duration_days";
    String DOMIBUS_ALERT_CERT_EXPIRED_LEVEL = "domibus.alert.cert.expired.level";
    String DOMIBUS_ALERT_CERT_EXPIRED_MAIL_SUBJECT = "domibus.alert.cert.expired.mail.subject";
    String DOMIBUS_ALERT_PASSWORD_IMMINENT_EXPIRATION_ACTIVE = "domibus.alert.password.imminent_expiration.active";
    String DOMIBUS_ALERT_PASSWORD_IMMINENT_EXPIRATION_DELAY_DAYS = "domibus.alert.password.imminent_expiration.delay_days";
    String DOMIBUS_ALERT_PASSWORD_IMMINENT_EXPIRATION_FREQUENCY_DAYS = "domibus.alert.password.imminent_expiration.frequency_days";
    String DOMIBUS_ALERT_PASSWORD_IMMINENT_EXPIRATION_LEVEL = "domibus.alert.password.imminent_expiration.level";
    String DOMIBUS_ALERT_PASSWORD_IMMINENT_EXPIRATION_MAIL_SUBJECT = "domibus.alert.password.imminent_expiration.mail.subject";
    String DOMIBUS_ALERT_PASSWORD_EXPIRED_ACTIVE = "domibus.alert.password.expired.active";
    String DOMIBUS_ALERT_PASSWORD_EXPIRED_DELAY_DAYS = "domibus.alert.password.expired.delay_days";
    String DOMIBUS_ALERT_PASSWORD_EXPIRED_FREQUENCY_DAYS = "domibus.alert.password.expired.frequency_days";
    String DOMIBUS_ALERT_PASSWORD_EXPIRED_LEVEL = "domibus.alert.password.expired.level";
    String DOMIBUS_ALERT_PASSWORD_EXPIRED_MAIL_SUBJECT = "domibus.alert.password.expired.mail.subject";
    String DOMIBUS_ALERT_PLUGIN_PASSWORD_IMMINENT_EXPIRATION_ACTIVE = "domibus.alert.plugin_password.imminent_expiration.active";
    String DOMIBUS_ALERT_PLUGIN_PASSWORD_IMMINENT_EXPIRATION_DELAY_DAYS = "domibus.alert.plugin_password.imminent_expiration.delay_days";
    String DOMIBUS_ALERT_PLUGIN_PASSWORD_IMMINENT_EXPIRATION_FREQUENCY_DAYS = "domibus.alert.plugin_password.imminent_expiration.frequency_days";
    String DOMIBUS_ALERT_PLUGIN_PASSWORD_IMMINENT_EXPIRATION_LEVEL = "domibus.alert.plugin_password.imminent_expiration.level";
    String DOMIBUS_ALERT_PLUGIN_PASSWORD_IMMINENT_EXPIRATION_MAIL_SUBJECT = "domibus.alert.plugin_password.imminent_expiration.mail.subject";
    String DOMIBUS_ALERT_PLUGIN_PASSWORD_EXPIRED_ACTIVE = "domibus.alert.plugin_password.expired.active";
    String DOMIBUS_ALERT_PLUGIN_PASSWORD_EXPIRED_DELAY_DAYS = "domibus.alert.plugin_password.expired.delay_days";
    String DOMIBUS_ALERT_PLUGIN_PASSWORD_EXPIRED_FREQUENCY_DAYS = "domibus.alert.plugin_password.expired.frequency_days";
    String DOMIBUS_ALERT_PLUGIN_PASSWORD_EXPIRED_LEVEL = "domibus.alert.plugin_password.expired.level";
    String DOMIBUS_ALERT_PLUGIN_PASSWORD_EXPIRED_MAIL_SUBJECT = "domibus.alert.plugin_password.expired.mail.subject";
    String DOMIBUS_ALERT_PLUGIN_USER_LOGIN_FAILURE_ACTIVE = "domibus.alert.plugin.user.login_failure.active";
    String DOMIBUS_ALERT_PLUGIN_USER_LOGIN_FAILURE_LEVEL = "domibus.alert.plugin.user.login_failure.level";
    String DOMIBUS_ALERT_PLUGIN_USER_LOGIN_FAILURE_MAIL_SUBJECT = "domibus.alert.plugin.user.login_failure.mail.subject";
    String DOMIBUS_ALERT_PLUGIN_USER_ACCOUNT_DISABLED_ACTIVE = "domibus.alert.plugin.user.account_disabled.active";
    String DOMIBUS_ALERT_PLUGIN_USER_ACCOUNT_DISABLED_LEVEL = "domibus.alert.plugin.user.account_disabled.level";
    String DOMIBUS_ALERT_PLUGIN_USER_ACCOUNT_DISABLED_MOMENT = "domibus.alert.plugin.user.account_disabled.moment";
    String DOMIBUS_ALERT_PLUGIN_USER_ACCOUNT_DISABLED_SUBJECT = "domibus.alert.plugin.user.account_disabled.subject";

    /**
     * Get all the properties metadata that support changing at runtime
     *
     * @return properties as metadata
     */
    Map<String, DomibusPropertyMetadata> getKnownProperties();

    /**
     * True if the manager handles the specified property
     *
     * @param name the name of the property
     * @return
     */
    boolean hasKnownProperty(String name);
}
