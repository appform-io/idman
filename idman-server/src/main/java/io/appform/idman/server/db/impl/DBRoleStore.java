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
import io.appform.idman.server.db.RoleStore;
import io.appform.idman.server.db.model.StoredRole;
import io.appform.idman.server.utils.Utils;
import io.dropwizard.hibernate.AbstractDAO;
import lombok.val;
import lombok.var;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public class DBRoleStore extends AbstractDAO<StoredRole> implements RoleStore {

    @Inject
    public DBRoleStore(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Optional<StoredRole> create(String serviceId, String displayName, String description) {
        val roleId = Utils.readableId(serviceId + "_" + displayName);
        var role = get(serviceId, roleId).orElse(null);
        if (null != role) {
            role.setDescription(description);
            role.setDeleted(false);
        }
        else {
            role = new StoredRole(roleId, serviceId, displayName, description);
        }
        return Optional.of(persist(role));
    }

    @Override
    public Optional<StoredRole> get(String serviceId, String roleId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredRole.class);
        return list(cr.select(root)
                            .where(cb.and(cb.equal(root.get(FieldNames.SERVICE_ID), serviceId),
                                          cb.equal(root.get("roleId"), roleId))))
                .stream()
                .findAny();
    }

    @Override
    public Optional<StoredRole> update(String serviceId, String roleId, String description) {
        var role = get(serviceId, roleId).orElse(null);
        if (null == role) {
            return Optional.empty();
        }
        else {
            role.setDescription(description);
        }
        return Optional.of(persist(role));
    }

    @Override
    public boolean delete(String serviceId, String roleId) {
        var role = get(serviceId, roleId).orElse(null);
        if (null == role) {
            return false;
        }
        role.setDeleted(true);
        return persist(role).isDeleted();
    }

    @Override
    public List<StoredRole> list(String serviceId, boolean includeDeleted) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredRole.class);
        val query = cr.select(root).orderBy(cb.asc(root.get("displayName")));
        if (includeDeleted) {
            return list(query.where(cb.equal(root.get(FieldNames.SERVICE_ID), serviceId)));
        }
        return list(query.where(cb.equal(root.get(FieldNames.SERVICE_ID), serviceId),
                                cb.equal(root.get(FieldNames.DELETED), false)));
    }

    @Override
    public List<StoredRole> get(Collection<String> roleIds) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredRole.class);
        val query = cr.select(root).orderBy(cb.asc(root.get("displayName")));
        return list(query.where(cb.and(
                cb.equal(root.get(FieldNames.DELETED), false),
                root.get(FieldNames.ROLE_ID).in(roleIds))));
    }

}
