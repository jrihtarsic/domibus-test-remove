package eu.domibus.common.dao.security;

import eu.domibus.common.model.security.UserEntityBase;
import eu.domibus.common.model.security.UserPasswordHistory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ion Perpegel
 * @since 4.1
 */
public interface UserPasswordHistoryDao<U extends UserEntityBase> {

    void savePassword(final U user, String passwordHash, LocalDateTime passwordDate);

    void removePasswords(final U user, int oldPasswordsToKeep);

    List<UserPasswordHistory> getPasswordHistory(final U user, int entriesCount);

}

