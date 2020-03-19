package eu.domibus.core.logging;

import eu.domibus.api.property.DomibusConfigurationService;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by Cosmin Baciu on 12-Oct-16.
 */
@RunWith(JMockit.class)
public class LogbackLoggingConfiguratorTest {

    @Mocked
    LoggerFactory LOG;


    @Injectable
    DomibusConfigurationService domibusConfigurationService;

    @Injectable
    String domibusConfigLocation = File.separator + "user";

    @Tested
    LogbackLoggingConfigurator logbackLoggingConfigurator;


    @Test
    public void testConfigureLoggingWithCustomFile(@Mocked System mock) throws Exception {
        new Expectations(logbackLoggingConfigurator) {{
            logbackLoggingConfigurator.getDefaultLogbackConfigurationFile();
            result = "/user/logback.xml";

            System.getProperty(anyString);
            result = "/user/mylogback.xml";
        }};

        logbackLoggingConfigurator.configureLogging();

        new Verifications() {{
            String fileLocation = null;
            logbackLoggingConfigurator.configureLogging(fileLocation = withCapture());
            times = 1;

            Assert.assertEquals("/user/mylogback.xml", fileLocation);
        }};
    }

    @Test
    public void testConfigureLoggingWithTheDefaultLogbackConfigurationFile(@Mocked System mock) throws Exception {
        new Expectations(logbackLoggingConfigurator) {{
            logbackLoggingConfigurator.getDefaultLogbackConfigurationFile();
            result = "/user/logback.xml";

            System.getProperty(anyString);
            result = null;
        }};

        logbackLoggingConfigurator.configureLogging();

        new Verifications() {{
            String fileLocation = null;
            logbackLoggingConfigurator.configureLogging(fileLocation = withCapture());
            times = 1;

            Assert.assertEquals("/user/logback.xml", fileLocation);
        }};
    }

    @Test
    public void testConfigureLoggingWithEmptyConfigLocation(final @Capturing Logger log) throws Exception {
        logbackLoggingConfigurator.configureLogging(null);

        new Verifications() {{
            log.warn(anyString);
            times = 1;
        }};
    }

    @Test
    public void testConfigureLoggingWithMissingLogFile(@Mocked File file) throws Exception {
        new Expectations(logbackLoggingConfigurator) {{
            new File(anyString).exists();
            result = false;
        }};

        logbackLoggingConfigurator.configureLogging("/user/logback.xml");

        new Verifications() {{
            logbackLoggingConfigurator.configureLogback(anyString);
            times = 0;
        }};
    }

    @Test
    public void testConfigureLoggingWithExistingLogFile(@Mocked File file) throws Exception {
        new Expectations(logbackLoggingConfigurator) {{
            new File(anyString).exists();
            result = true;

            logbackLoggingConfigurator.configureLogback(anyString);
            result = null;
        }};

        logbackLoggingConfigurator.configureLogging("/user/logback.xml");

        new Verifications() {{
            logbackLoggingConfigurator.configureLogback(anyString);
            times = 1;
        }};
    }

    @Test
    public void testGetDefaultLogbackConfigurationFileWithConfiguredDomibusLocation() throws Exception {
        String defaultLogbackConfigurationFile = logbackLoggingConfigurator.getDefaultLogbackConfigurationFile();

        Assert.assertEquals(File.separator + "user" + File.separator + "logback.xml", defaultLogbackConfigurationFile);
    }

    @Test
    public void testGetDefaultLogbackFilePathWithMissingDomibusLocation(@Mocked System mock, final @Capturing Logger log) throws Exception {
        logbackLoggingConfigurator.domibusConfigLocation = null;
        logbackLoggingConfigurator.getDefaultLogbackConfigurationFile();

        new Verifications() {{
            log.error(anyString);
            times = 1;
        }};
    }

}
