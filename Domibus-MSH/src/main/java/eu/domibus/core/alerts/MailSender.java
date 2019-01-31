package eu.domibus.core.alerts;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.model.service.MailModel;
import eu.domibus.core.alerts.service.MultiDomainAlertConfigurationService;
import eu.domibus.logging.DomibusLoggerFactory;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@Component
public class MailSender {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(MailSender.class);

    static final String DOMIBUS_ALERT_SENDER_SMTP_URL = "domibus.alert.sender.smtp.url";

    static final String DOMIBUS_ALERT_SENDER_SMTP_PORT = "domibus.alert.sender.smtp.port";

    static final String DOMIBUS_ALERT_SENDER_SMTP_USER = "domibus.alert.sender.smtp.user";

    public static final String DOMIBUS_ALERT_SENDER_SMTP_PASSWORD = "domibus.alert.sender.smtp.password";

    static final String DOMIBUS_ALERT_MAIL = "domibus.alert.mail";

    private static final String MAIL = ".mail";


    @Autowired
    private Configuration freemarkerConfig;

    @Autowired
    private JavaMailSenderImpl javaMailSender;

    @Autowired
    private DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected DomainContextProvider domainProvider;

    @Autowired
    private MultiDomainAlertConfigurationService multiDomainAlertConfigurationService;

    private boolean mailSenderInitiated;

    protected void initMailSender() {
        final Boolean alertModuleEnabled = multiDomainAlertConfigurationService.isAlertModuleEnabled();
        LOG.debug("Alert module enabled:[{}]", alertModuleEnabled);
        final String sendEmailActivePropertyName = multiDomainAlertConfigurationService.getSendEmailActivePropertyName();
        final boolean mailActive = Boolean.parseBoolean(domibusPropertyProvider.getOptionalDomainProperty(sendEmailActivePropertyName));
        if (alertModuleEnabled && mailActive) {
            //static properties.
            final String url = domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_SMTP_URL);
            final Integer port = Integer.valueOf(domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_SMTP_PORT));
            final String user = domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_SMTP_USER);
            final String password = domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_SMTP_PASSWORD);

            LOG.debug("Configuring mail server.");
            LOG.debug("Smtp url:[{}]", url);
            LOG.debug("Smtp port:[{}]", port);
            LOG.debug("Smtp user:[{}]", user);

            javaMailSender.setHost(url);
            javaMailSender.setPort(port);
            javaMailSender.setUsername(user);
            javaMailSender.setPassword(password);
            //Non static properties.
            final Properties javaMailProperties = javaMailSender.getJavaMailProperties();
            final Set<String> mailPropertyNames = domibusPropertyProvider.filterPropertiesName(s -> s.startsWith(DOMIBUS_ALERT_MAIL));
            mailPropertyNames.
                    forEach(domibusPropertyName -> {
                        final String mailPropertyName = domibusPropertyName.substring(domibusPropertyName.indexOf(MAIL) + 1);
                        final String propertyValue = domibusPropertyProvider.getProperty(domibusPropertyName);
                        LOG.debug("mail property:[{}] value:[{}]", mailPropertyName, propertyValue);
                        javaMailProperties.put(mailPropertyName, propertyValue);
                    });
        }
    }

    public <T extends MailModel> void sendMail(final T model, final String from, final String to) {
        if(StringUtils.isBlank(to)) {
            throw new IllegalArgumentException("The 'to' property cannot be null");
        }
        if(StringUtils.isBlank(from)) {
            throw new IllegalArgumentException("The 'from' property cannot be null");
        }

        if (!mailSenderInitiated) {
            mailSenderInitiated = true;
            try {
                initMailSender();
            } catch (Exception ex) {
                LOG.error("Could not initiate mail sender", ex);
            }

        }
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = getMimeMessageHelper(message);

            Template template = freemarkerConfig.getTemplate(model.getTemplatePath());
            final Object model1 = model.getModel();
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model1);

            if (to.contains(";")) {
                String[] targets = to.split(";");
                helper.setTo(targets);
            } else {
                helper.setTo(to);
            }
            helper.setText(html, true);
            helper.setSubject(model.getSubject());
            helper.setFrom(from);
            javaMailSender.send(message);
        } catch (IOException | MessagingException | TemplateException e) {
            LOG.error("Exception while sending mail from[{}] to[{}]", from, to, e);
            throw new AlertDispatchException(e);
        }
    }

    MimeMessageHelper getMimeMessageHelper(MimeMessage message) throws MessagingException {
        return new MimeMessageHelper(message,
                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                        StandardCharsets.UTF_8.name());
    }


}
