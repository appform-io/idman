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
import io.appform.idman.server.db.SessionStoreForType;
import io.appform.idman.server.db.model.ClientSession;
import io.appform.idman.model.TokenType;
import io.appform.idman.server.db.model.StoredStaticSession;
import io.dropwizard.hibernate.AbstractDAO;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * For storing sessions in a database
 */
@Singleton
public class DBStaticSessionStore extends AbstractDAO<StoredStaticSession> implements SessionStoreForType {

    @Inject
    public DBStaticSessionStore(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    @SneakyThrows
    public Optional<ClientSession> create(
            String sessionId,
            String userId,
            String serviceId,
            String clientSessionId,
            Date expiry) {
        if(null != expiry) {
            throw new IllegalArgumentException("Static Session does not have expiry");
        }
        try {
            return Optional.of(persist(new StoredStaticSession(sessionId, userId, serviceId)))
                    .map(DBStaticSessionStore::toWire);
        }
        catch (ConstraintViolationException e) {
            throw new IllegalArgumentException("Session exists");
        }
    }

    @Override
    public Optional<ClientSession> get(String sessionId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredStaticSession.class);
        return list(cr.select(root).where(
                cb.and(cb.equal(root.get("sessionId"), sessionId),
                       cb.equal(root.get(FieldNames.DELETED), false))))
                .stream()
                .findAny()
                .map(DBStaticSessionStore::toWire);
    }

    @Override
    public List<ClientSession> sessionsForUser(String userId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredStaticSession.class);
        return list(cr.select(root)
                            .where(cb.and(
                                    cb.equal(root.get(FieldNames.USER_ID), userId),
                                    cb.equal(root.get("deleted"), false)))
                            .orderBy(cb.desc(root.get("updated"))))
                .stream()
                .map(DBStaticSessionStore::toWire)
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String sessionId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredStaticSession.class);
        val session = list(cr.select(root).where(cb.equal(root.get("sessionId"), sessionId)))
                .stream()
                .findAny()
                .orElse(null);
        if (null == session) {
            return false;
        }
        session.setDeleted(true);
        return persist(session).isDeleted();
    }

    private static ClientSession toWire(final StoredStaticSession dynamicSession) {
        return new ClientSession(
                dynamicSession.getSessionId(),
                dynamicSession.getUserId(),
                dynamicSession.getServiceId(),
                null,
                TokenType.STATIC,
                null,
                dynamicSession.isDeleted(),
                dynamicSession.getCreated(),
                dynamicSession.getUpdated()
        );
    }
}
