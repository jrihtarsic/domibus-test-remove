package eu.domibus.plugin.webService.dao;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;

/**
 * A basic DAO implementation providing the standard CRUD operations.
 * Reuses the Domibus EntityManager.
 *
 * @author idragusa
 * @since 4.2
 */
public abstract class BasicDao<T> {

    protected final Class<T> typeOfT;

    @PersistenceContext(unitName = "domibusJTA")
    protected EntityManager em;

    /**
     * @param typeOfT The entity class this DAO provides access to
     */
    public BasicDao(final Class<T> typeOfT) {
        this.typeOfT = typeOfT;
    }

    public <T> T findById(Class<T> typeOfT, String id) {
        return em.find(typeOfT, id);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void create(final T entity) {
        em.persist(entity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(final T entity) {
        em.remove(em.merge(entity));
    }

    public T read(final long id) {
        return em.find(this.typeOfT, id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateAll(final Collection<T> update) {
        for (final T t : update) {
            this.update(t);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAll(final Collection<T> delete) {
        for (final T t : delete) {
            this.delete(t);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void update(final T entity) {
        em.merge(entity);
    }

    public void flush() {
        em.flush();
    }

}
