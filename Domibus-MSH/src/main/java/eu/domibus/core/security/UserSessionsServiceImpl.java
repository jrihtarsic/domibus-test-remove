package eu.domibus.core.security;

import eu.domibus.api.multitenancy.UserSessionsService;
import eu.domibus.api.user.UserBase;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.security.UserDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation for {@link UserSessionsService}
 * It is located in core package(and nor web) because it is referred from classes created in root application context
 *
 * @author Ion Perpegel
 * @since 4.2
 */
@Service
public class UserSessionsServiceImpl implements UserSessionsService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserSessionsServiceImpl.class);

    @Autowired
    SessionRegistry sessionRegistry;

    @Override
    public void invalidateSessions(UserBase user) {
        LOG.debug("Invalidate sessions called for user [{}]", user.getUserName());
        Optional<UserDetail> userDetail = sessionRegistry.getAllPrincipals().stream()
                .map(p -> ((UserDetail) p))
                .filter(u -> u.getUsername().equals(user.getUserName()))
                .findFirst();
        if (userDetail.isPresent()) {
            LOG.info("Found session for user [{}]", user.getUserName());
            List<SessionInformation> sess = sessionRegistry.getAllSessions(userDetail.get(), false);
            sess.forEach(session -> {
                LOG.info("Expire session [{}] for user [{}]", session, user.getUserName());
                session.expireNow();
            });
        }
    }
}
