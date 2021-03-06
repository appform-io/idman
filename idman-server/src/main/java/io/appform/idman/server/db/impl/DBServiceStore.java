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
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.db.model.StoredService;
import io.appform.idman.server.utils.Utils;
import io.dropwizard.hibernate.AbstractDAO;
import lombok.val;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 *
 */
public class DBServiceStore extends AbstractDAO<StoredService> implements ServiceStore {

    @Inject
    public DBServiceStore(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Optional<StoredService> create(String name, String description, String callbackUrl) {
        val id = Utils.readableId(name);
        var service = get(id).orElse(null);
        val secret = UUID.randomUUID().toString();
        if (null == service) {
            service = new StoredService(id, name, description, callbackUrl, secret);
        }
        else {
            service.setDescription(description);
            service.setCallbackUrl(callbackUrl);
            service.setSecret(secret);
            service.setDeleted(false);
        }
        return Optional.of(persist(service));
    }

    @Override
    public Optional<StoredService> get(String serviceId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredService.class);
        return list(cr.select(root).where(cb.equal(root.get("serviceId"), serviceId)))
                .stream()
                .findAny();
    }

    @Override
    public Optional<StoredService> updateDescription(String serviceId, String description) {
        return updateService(serviceId, service -> service.setDescription(description));
    }

    @Override
    public Optional<StoredService> updateCallbackUrl(String serviceId, String callbackUrl) {
        return updateService(serviceId, service -> service.setCallbackUrl(callbackUrl));
    }

    @Override
    public Optional<StoredService> updateSecret(String serviceId) {
        return updateService(serviceId, service -> service.setSecret(UUID.randomUUID().toString()));
    }

    @Override
    public boolean delete(String serviceId) {
        val service = get(serviceId).orElse(null);
        if (null == service) {
            return false;
        }
        service.setDeleted(true);
        return persist(service).isDeleted();
    }

    @Override
    public List<StoredService> get(Collection<String> serviceIds) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredService.class);
        val query = cr.select(root).orderBy(cb.asc(root.get("name")));
        return list(query.where(cb.and(
                cb.equal(root.get(FieldNames.DELETED), false),
                root.get(FieldNames.SERVICE_ID).in(serviceIds))));

    }

    @Override
    public List<StoredService> list(boolean includeDeleted) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredService.class);
        val query = cr.select(root).orderBy(cb.asc(root.get("name")));
        if (!includeDeleted) {
            list(query.where(cb.equal(root.get("deleted"), false)));
        }
        return list(query);
    }

    private Optional<StoredService> updateService(String serviceId, Consumer<StoredService> handler) {
        val service = get(serviceId).orElse(null);
        if (null == service) {
            return Optional.empty();
        }
        handler.accept(service);
        return Optional.of(persist(service));
    }

}
