/*
 * Copyright 2021. Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package io.appform.idman.server.db.impl;

import io.appform.idman.server.db.FieldNames;
import io.appform.idman.server.db.UserRoleStore;
import io.appform.idman.server.db.model.StoredUserRole;
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
public class DBUserRoleStore extends AbstractDAO<StoredUserRole> implements UserRoleStore {

    @Inject
    public DBUserRoleStore(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public void mapUserToRole(String userId, String serviceId, String roleId, String assignedBy) {
        var mapping = getMapping(userId, serviceId);
        if (null == mapping) {
            mapping = new StoredUserRole(userId, serviceId, roleId, assignedBy);
        }
        else {
            mapping.setRoleId(roleId);
            mapping.setAssignedBy(assignedBy);
            mapping.setDeleted(false);
        }
        persist(mapping);
    }

    @Override
    public boolean unmapUserFromRole(String userId, String serviceId) {
        var mapping = getMapping(userId, serviceId);
        if (null == mapping) {
            return false;
        }
        else {
            mapping.setDeleted(true);
        }
        persist(mapping);
        return true;
    }

    @Override
    public List<StoredUserRole> getUserRoles(String userId) {
        return list((cr, cb, root) -> cb.and(
                cb.equal(root.get(FieldNames.USER_ID), userId),
                cb.equal(root.get(FieldNames.DELETED), false)));
    }

    @Override
    public List<StoredUserRole> getServiceRoleMappings(String serviceId) {
        return list((cr, cb, root) -> cb.and(
                cb.equal(root.get(FieldNames.SERVICE_ID), serviceId),
                cb.equal(root.get(FieldNames.DELETED), false)));
    }

    @Override
    public Optional<StoredUserRole> getUserServiceRole(String userId, String serviceId) {
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

    private List<StoredUserRole> list(QueryGenerator<StoredUserRole> generator) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredUserRole.class);
        return list(cr.select(root).where(generator.generate(cr, cb, root)));
    }

    private StoredUserRole getMapping(String userId, String serviceId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredUserRole.class);
        return list(cr.select(root).where(cb.and(cb.equal(root.get(FieldNames.SERVICE_ID), serviceId),
                                                 cb.equal(root.get(FieldNames.USER_ID), userId))))
                .stream()
                .findAny()
                .orElse(null);
    }

}
