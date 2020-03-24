package eu.domibus.core.alerts.model.mapper;

import eu.domibus.core.alerts.model.common.AuthenticationEvent;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.alerts.model.mapper.EventMapperImpl;
import eu.domibus.core.alerts.model.mapper.EventMapperImpl_;
import eu.domibus.core.alerts.model.service.Event;
import eu.domibus.core.alerts.model.mapper.EventMapper;

import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Thomas Dussart, Ioana Dragusanu
 * @since 4.0
 */

@RunWith(JMockit.class)
public class AlertBeanConversionTest {

    @Test
    public void testConversion() throws ParseException {
        final String user = "user";
        final String accountDisabled = "false";

        SimpleDateFormat parser = new SimpleDateFormat("dd/mm/yyy HH:mm:ss");
        final Date reportingTime = parser.parse("25/10/2001 00:00:00");
        final Date loginTime = parser.parse("26/10/2001 00:00:00");


        Event event = new Event();
        event.setEntityId(1);
        event.setType(EventType.USER_LOGIN_FAILURE);
        event.setReportingTime(reportingTime);

        event.addDateKeyValue(AuthenticationEvent.LOGIN_TIME.name(), loginTime);
        event.addStringKeyValue(AuthenticationEvent.USER.name(), user);
        event.addStringKeyValue(AuthenticationEvent.ACCOUNT_DISABLED.name(), accountDisabled);

        EventMapperImpl eventMapper = new EventMapperImpl();
        eventMapper.setDelegate(new EventMapperImpl_());
        final eu.domibus.core.alerts.model.persist.Event persistEvent = eventMapper.eventServiceToEventPersist(event);
        Assert.assertEquals(1, persistEvent.getEntityId());
        Assert.assertEquals(EventType.USER_LOGIN_FAILURE, persistEvent.getType());
        Assert.assertEquals(reportingTime, persistEvent.getReportingTime());

        Assert.assertEquals(loginTime, persistEvent.getProperties().get(AuthenticationEvent.LOGIN_TIME.name()).getValue());
        Assert.assertEquals(user, persistEvent.getProperties().get(AuthenticationEvent.USER.name()).getValue());
        Assert.assertEquals(accountDisabled, persistEvent.getProperties().get(AuthenticationEvent.ACCOUNT_DISABLED.name()).getValue());
    }
}
