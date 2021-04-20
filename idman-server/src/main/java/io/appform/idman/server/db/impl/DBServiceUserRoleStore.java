package io.appform.idman.server.db.impl;

import io.appform.idman.server.db.FieldNames;
import io.appform.idman.server.db.ServiceUserRoleStore;
import io.appform.idman.server.db.model.StoredServiceUserRole;
import io.dropwizard.hibernate.AbstractDAO;
import lombok.val;
import lombok.var;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public class DBServiceUserRoleStore extends AbstractDAO<StoredServiceUserRole> implements ServiceUserRoleStore {

    @Inject
    public DBServiceUserRoleStore(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public boolean mapUserToRole(String userId, String serviceId, String roleId, String assignedBy) {
        var mapping = getMapping(userId, serviceId);
        if (null == mapping) {
            mapping = new StoredServiceUserRole(userId, serviceId, roleId, assignedBy);
        }
        else {
            mapping.setRoleId(roleId);
            mapping.setAssignedBy(assignedBy);
            mapping.setDeleted(false);
        }
        return !persist(mapping).isDeleted();
    }

    @Override
    public boolean unmapUserFromRole(String userId, String serviceId, String roleId) {
        var mapping = getMapping(userId, serviceId);
        if (null == mapping) {
            return false;
        }
        else {
            mapping.setDeleted(true);
        }
        return !persist(mapping).isDeleted();
    }

    @Override
    public List<StoredServiceUserRole> getUserRoles(String userId) {
        return list((cr, cb, root) -> cb.and(
                cb.equal(root.get(FieldNames.USER_ID), userId),
                cb.equal(root.get(FieldNames.DELETED), false)));
    }

    @Override
    public List<StoredServiceUserRole> getServiceRoleMappings(String serviceId) {
        return list((cr, cb, root) -> cb.and(
                cb.equal(root.get(FieldNames.SERVICE_ID), serviceId),
                cb.equal(root.get(FieldNames.DELETED), false)));
    }

    @Override
    public Optional<StoredServiceUserRole> getUserServiceRole(String userId, String serviceId) {
        return list((cr, cb, root) -> cb.and(
                cb.equal(root.get(FieldNames.SERVICE_ID), serviceId),
                cb.equal(root.get(FieldNames.USER_ID), userId),
                cb.equal(root.get(FieldNames.DELETED), false)))
                .stream()
                .findAny();
    }

    @FunctionalInterface
    private interface QueryGenerator<T> {
        Expression<Boolean> generate(CriteriaQuery<T> cr, CriteriaBuilder cb, Root<T> root);
    }

    private List<StoredServiceUserRole> list(QueryGenerator<StoredServiceUserRole> generator) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredServiceUserRole.class);
        return list(cr.select(root).where(generator.generate(cr, cb, root)));
    }

    private StoredServiceUserRole getMapping(String userId, String serviceId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredServiceUserRole.class);
        return list(cr.select(root).where(cb.and(cb.equal(root.get(FieldNames.SERVICE_ID), serviceId),
                                                 cb.equal(root.get(FieldNames.USER_ID), userId))))
                .stream()
                .findAny()
                .orElse(null);
    }

}
