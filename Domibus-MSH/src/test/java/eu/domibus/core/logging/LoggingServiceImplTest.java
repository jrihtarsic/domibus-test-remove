package eu.domibus.core.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import eu.domibus.api.cluster.SignalService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.jms.JMSMessageBuilder;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.logging.LogbackLoggingConfigurator;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Catalin Enache
 * @since 4.1
 */
@RunWith(JMockit.class)
public class LoggingServiceImplTest {

    @Injectable
    protected DomainCoreConverter domainConverter;

    @Injectable
    protected DomibusConfigurationService domibusConfigurationService;

    @Injectable
    protected SignalService signalService;

    @Tested
    LoggingServiceImpl loggingService;

    @Test
    public void testSetLoggingLevel_LevelNotNull_LoggerLevelSet() {
        final String name = "eu.domibus";
        final String level = "INFO";

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        new Expectations(loggingService) {{
            loggingService.toLevel(level);
            result = Level.DEBUG;
        }};

        //tested method
        loggingService.setLoggingLevel(name, level);

        Assert.assertEquals(Level.DEBUG, loggerContext.getLogger(name).getLevel());
    }

    @Test
    public void testSetLoggingLevel_LevelIsRoot_LoggerLevelSet() {
        final String name = "root";
        final String level = "INFO";

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        new Expectations(loggingService) {{
            loggingService.toLevel(level);
            result = Level.INFO;
        }};

        //tested method
        loggingService.setLoggingLevel(name, level);

        Assert.assertEquals(Level.INFO, loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getLevel());
    }

    @Test
    public void testSetLoggingLevel_LevelNotRecognized_ExceptionThrown() {
        final String name = "eu.domibus";
        final String level = "BLA";

        try {
            //tested method
            loggingService.setLoggingLevel(name, level);
            Assert.fail("LoggingException expected");
        } catch (LoggingException le) {
            Assert.assertEquals(DomibusCoreErrorCode.DOM_001, le.getError());
            Assert.assertTrue(le.getMessage().contains("Not a known log level"));
        }
    }



    @Test
    public void testSignalSetLoggingLevel_NoException_MessageSent() {
        final String name = "eu.domibus";
        final String level = "INFO";


        //tested method
        loggingService.signalSetLoggingLevel(name, level);

        new Verifications() {{
            String actualName, actualLevel;
            signalService.signalLoggingSetLevel(actualName = withCapture(), actualLevel = withCapture());
            Assert.assertEquals(name, actualName);
            Assert.assertEquals(level, actualLevel);
        }};
    }


    @Test
    public void testSignalSetLoggingLevel_ExceptionThrown_MessageNotSent() {
        final String name = "eu.domibus";
        final String level = "INFO";

        new Expectations(loggingService) {{
            signalService.signalLoggingSetLevel(name, level);
            result = new LoggingException("Error while sending topic message for setting logging level");
        }};

        try {
            //tested method
            loggingService.signalSetLoggingLevel(name, level);
            Assert.fail("LoggingException expected");
        } catch (LoggingException le) {
            Assert.assertEquals(DomibusCoreErrorCode.DOM_001, le.getError());
            Assert.assertTrue(le.getMessage().contains("Error while sending topic message for setting logging level"));
        }
    }

    @Test
    public void testGetLoggingLevel_LoggerNameExact_ListReturned() {
        final String name = "eu.domibus";
        final boolean showClasses = false;

        //tested method
        List<LoggingEntry> loggingEntries = loggingService.getLoggingLevel(name, showClasses);

        Assert.assertTrue(CollectionUtils.isNotEmpty(loggingEntries));
        Assert.assertTrue(loggingEntries.get(0).getName().startsWith(name));
    }

    @Test
    public void testGetLoggingLevel_LoggerNameContainsWith_ListReturned() {
        final String name = "omibu";
        final boolean showClasses = false;

        //tested method
        List<LoggingEntry> loggingEntries = loggingService.getLoggingLevel(name, showClasses);

        Assert.assertTrue(CollectionUtils.isNotEmpty(loggingEntries));
        Assert.assertTrue(loggingEntries.get(0).getName().contains("domibus"));
    }

    @Test
    public void testResetLogging(final @Mocked LogbackLoggingConfigurator logbackLoggingConfigurator) {

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        String domibusConfigLocation = "/home";//TODO

        new Expectations(loggingService) {{
            domibusConfigurationService.getConfigLocation();
            result = domibusConfigLocation;

            new LogbackLoggingConfigurator(domibusConfigLocation);
            result = logbackLoggingConfigurator;

            logbackLoggingConfigurator.getLoggingConfigurationFile();
            result = this.getClass().getResource("/logback-test.xml").getPath();
        }};

        //tested method
        loggingService.resetLogging();
        Assert.assertEquals(Level.ERROR, context.getLogger("com.atomikos").getLevel());

    }


    @Test
    public void testSignalResetLogging_NoException_MessageSent() {

        //tested method
        loggingService.signalResetLogging();

        new Verifications() {{
            signalService.signalLoggingReset();
        }};
    }

//    @Test
//    public void testSignalLoggingReset_ExceptionThrown_MessageNotSent(final @Mocked JmsMessage jmsMessage, final @Mocked JMSMessageBuilder messageBuilder) {
//        final String name = "eu.domibus";
//        final String level = "INFO";
//
//        new Expectations(loggingService) {{
//            JMSMessageBuilder.create();
//            result = messageBuilder;
//
//            messageBuilder.property(Command.COMMAND, Command.LOGGING_RESET);
//            result = messageBuilder;
//
//            messageBuilder.build();
//            result = jmsMessage;
//
//            jmsManager.sendMessageToTopic(jmsMessage, clusterCommandTopic);
//            result = new DestinationResolutionException("error while sending JMS message");
//        }};
//
//        try {
//            //tested method
//            loggingService.signalResetLogging();
//            Assert.fail("LoggingException expected");
//        } catch (LoggingException le) {
//            Assert.assertEquals(DomibusCoreErrorCode.DOM_001, le.getError());
//            Assert.assertTrue(le.getMessage().contains("Error while sending topic message for logging reset"));
//        }
//    }

    @Test
    public void testSignalLoggingReset_ExceptionThrown_MessageNotSent(final @Mocked JmsMessage jmsMessage, final @Mocked JMSMessageBuilder messageBuilder) {
        final String name = "eu.domibus";
        final String level = "INFO";

        new Expectations(loggingService) {{
//            JMSMessageBuilder.create();
//            result = messageBuilder;
//
//            messageBuilder.property(Command.COMMAND, Command.LOGGING_RESET);
//            result = messageBuilder;
//
//            messageBuilder.build();
//            result = jmsMessage;
//
//            jmsManager.sendMessageToTopic(jmsMessage, clusterCommandTopic);
//            result = new DestinationResolutionException("error while sending JMS message");
            signalService.signalLoggingReset();
            result = new LoggingException("Error while sending topic message for logging reset");
        }};

        try {
            //tested method
            loggingService.signalResetLogging();
            Assert.fail("LoggingException expected");
        } catch (LoggingException le) {
            Assert.assertEquals(DomibusCoreErrorCode.DOM_001, le.getError());
            Assert.assertTrue(le.getMessage().contains("Error while sending topic message for logging reset"));
        }
    }

    @Test
    public void testToLevel(){
        String level = "ALL";
        Assert.assertEquals(Level.ALL, loggingService.toLevel(level));

        level = "TRACE";
        Assert.assertEquals(Level.TRACE, loggingService.toLevel(level));

        level = "DEBUG";
        Assert.assertEquals(Level.DEBUG, loggingService.toLevel(level));

        level = "INFO";
        Assert.assertEquals(Level.INFO, loggingService.toLevel(level));

        level = "ERROR";
        Assert.assertEquals(Level.ERROR, loggingService.toLevel(level));

        level = "ALL";
        Assert.assertEquals(Level.ALL, loggingService.toLevel(level));

        try {
            loggingService.toLevel(null);
            Assert.fail("LoggingException expected");
        } catch (LoggingException le) {
            Assert.assertEquals(DomibusCoreErrorCode.DOM_001, le.getError());
        }

        try {
            loggingService.toLevel("BLABLA");
            Assert.fail("LoggingException expected");
        } catch (LoggingException le) {
            Assert.assertEquals(DomibusCoreErrorCode.DOM_001, le.getError());
        }

    }
}