package eu.domibus.common.dao.security;

import eu.domibus.AbstractIT;
import eu.domibus.common.model.security.User;
import eu.domibus.common.model.security.UserRole;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
public class UserDaoImplTestIT extends AbstractIT {
    @Autowired
    private UserDao userDao;

    @PersistenceContext(unitName = "domibusJTA")
    protected EntityManager entityManager;

    @Test
    @Transactional
    @Rollback
    public void listUsers() throws Exception {
        User user = new User();
        user.setUserName("userOne");
        user.setPassword("test");
        UserRole userRole = new UserRole("ROLE_USER");
        entityManager.persist(userRole);
        user.addRole(userRole);
        user.setEmail("test@gmail.com");
        user.setActive(true);
        userDao.create(user);
        List<User> users = userDao.listUsers();
        assertEquals(1, users.size());
        user = users.get(0);
        assertEquals("test@gmail.com", user.getEmail());
        assertEquals("test", user.getPassword());
        assertEquals(true, user.isActive());
    }

    @Test
    @Transactional
    @Rollback
    public void loadActiveUserByUsername() {
        User user = new User();
        user.setUserName("userTwo");
        user.setPassword("test");
        UserRole userRole = new UserRole("ROLE_USER_2");
        entityManager.persist(userRole);
        user.addRole(userRole);
        user.setEmail("test@gmail.com");
        user.setActive(true);
        userDao.create(user);
        final User userOne = userDao.loadActiveUserByUsername("userTwo");
        assertNotNull(userOne);
    }

}