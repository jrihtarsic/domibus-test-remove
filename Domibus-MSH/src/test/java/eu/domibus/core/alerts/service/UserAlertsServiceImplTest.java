package eu.domibus.core.alerts.service;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.converters.UserConverter;
import eu.domibus.common.dao.security.UserDao;
import eu.domibus.common.dao.security.UserPasswordHistoryDao;
import eu.domibus.common.dao.security.UserRoleDao;
import eu.domibus.common.model.security.IUser;
import eu.domibus.common.model.security.User;
import eu.domibus.common.services.UserPersistenceService;
import eu.domibus.core.alerts.model.common.AlertType;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@RunWith(JMockit.class)
public class UserAlertsServiceImplTest {

    @Injectable
    private UserDao userDao;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private UserRoleDao userRoleDao;

    @Injectable
    protected UserPasswordHistoryDao userPasswordHistoryDao;

    @Injectable
    private UserPersistenceService userPersistenceService;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private UserConverter userConverter;

    @Injectable
    private MultiDomainAlertConfigurationService multiDomainAlertConfigurationService;

    @Injectable
    private EventService eventService;

    @Injectable
    protected UserDomainService userDomainService;

    @Injectable
    protected DomainService domainService;

    @Tested
    private UserAlertsServiceImpl userAlertsService;

    @Test
    public void testSendPasswordExpiredAlerts() {
        final LocalDate today = LocalDate.of(2018, 10, 15);
        final Integer maxPasswordAge = 10;
        final Integer howManyDaysToGenerateAlertsAfterExpiration = 3;
        final LocalDate from = LocalDate.of(2018, 10, 2);
        final LocalDate to = LocalDate.of(2018, 10, 5);
        final User user1 = new User("user1", "anypassword");
        final User user2 = new User("user2", "anypassword");
        final List<User> users = Arrays.asList(user1, user2);

        new Expectations(LocalDate.class) {{
            LocalDate.now();
            result = today;
        }};
        new Expectations() {{
            userAlertsService.getAlertTypeForPasswordExpired();
            result = AlertType.PASSWORD_EXPIRED;
            multiDomainAlertConfigurationService.getRepetitiveEventConfiguration(AlertType.PASSWORD_EXPIRED).isActive();
            result = true;
            multiDomainAlertConfigurationService.getRepetitiveEventConfiguration(AlertType.PASSWORD_EXPIRED).getEventDelay();
            result = howManyDaysToGenerateAlertsAfterExpiration;
            userAlertsService.getMaximumPasswordAgeProperty();
            result = ConsoleUserAlertsServiceImpl.MAXIMUM_PASSWORD_AGE;
            domibusPropertyProvider.getOptionalDomainProperty(ConsoleUserAlertsServiceImpl.MAXIMUM_PASSWORD_AGE);
            result = maxPasswordAge.toString();
            userAlertsService.getUsersWithPasswordChangedBetween(false, from, to);
//            userDao.findWithPasswordChangedBetween(from, to, false);
            result = users;
        }};

        userAlertsService.sendExpiredAlerts(false);

        new VerificationsInOrder() {{
            eventService.enqueuePasswordExpiredEvent((User) any, maxPasswordAge);
            times = 2;
        }};
    }

    @Test
    public void testSendPasswordImminentExpirationAlerts() {
        final LocalDate today = LocalDate.of(2018, 10, 15);
        final Integer maxPasswordAge = 10;
        final Integer howManyDaysBeforeExpirationToGenerateAlerts = 4;
        final LocalDate from = LocalDate.of(2018, 10, 5);
        final LocalDate to = LocalDate.of(2018, 10, 9);
        final IUser user1 = new User("user1", "anypassword");
        final IUser user2 = new User("user2", "anypassword");
        final List<IUser> users = Arrays.asList(user1, user2);

        new Expectations(LocalDate.class) {{
            LocalDate.now();
            result = today;
        }};
        new Expectations() {{
            userAlertsService.getAlertTypeForPasswordImminentExpiration();
            result = AlertType.PASSWORD_IMMINENT_EXPIRATION;
            userAlertsService.getMaximumPasswordAgeProperty();
            result = ConsoleUserAlertsServiceImpl.MAXIMUM_PASSWORD_AGE;
            multiDomainAlertConfigurationService.getRepetitiveEventConfiguration(AlertType.PASSWORD_IMMINENT_EXPIRATION).isActive();
            result = true;
            multiDomainAlertConfigurationService.getRepetitiveEventConfiguration(AlertType.PASSWORD_IMMINENT_EXPIRATION).getEventDelay();
            result = howManyDaysBeforeExpirationToGenerateAlerts;
            domibusPropertyProvider.getOptionalDomainProperty(ConsoleUserAlertsServiceImpl.MAXIMUM_PASSWORD_AGE);
            result = maxPasswordAge.toString();
            userAlertsService.getUsersWithPasswordChangedBetween(false, from, to);
            result = users;
        }};

        userAlertsService.sendImminentExpirationAlerts(false);

        new VerificationsInOrder() {{
            eventService.enqueuePasswordImminentExpirationEvent((IUser) any, maxPasswordAge);
            times = 2;
        }};
    }


    @Test
    public void testSendPasswordAlerts() {

        userAlertsService.sendAlerts();

        new VerificationsInOrder() {{
            userAlertsService.sendImminentExpirationAlerts(false);
            times = 1;
        }};
        new VerificationsInOrder() {{
            userAlertsService.sendExpiredAlerts(false);
            times = 1;
        }};
    }

}
