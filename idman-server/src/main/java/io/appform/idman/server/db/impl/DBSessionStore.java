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
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.model.SessionType;
import io.appform.idman.server.db.model.StoredUserSession;
import io.dropwizard.hibernate.AbstractDAO;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * For storing sessions in a database
 */
public class DBSessionStore extends AbstractDAO<StoredUserSession> implements SessionStore {

    @Inject
    public DBSessionStore(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    @SneakyThrows
    public Optional<StoredUserSession> create(
            String sessionId,
            String userId,
            String serviceId,
            String clientSessionId,
            SessionType type,
            Date expiry) {
        try {
            return Optional.of(persist(new StoredUserSession(sessionId,
                                                             userId,
                                                             serviceId,
                                                             clientSessionId,
                                                             type,
                                                             expiry)));
        }
        catch (ConstraintViolationException e) {
            throw new IllegalArgumentException("Session exists");
        }
    }

    @Override
    public Optional<StoredUserSession> get(String sessionId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredUserSession.class);
        return list(cr.select(root).where(
                cb.and(cb.equal(root.get("sessionId"), sessionId),
                       cb.equal(root.get(FieldNames.DELETED), false))))
                .stream()
                .findAny();
    }

    @Override
    public List<StoredUserSession> sessionsForUser(String userId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredUserSession.class);
        return list(cr.select(root)
                            .where(
                                    cb.and(
                                            cb.equal(root.get(FieldNames.USER_ID), userId),
                                            cb.equal(root.get("deleted"), false),
                                            cb.or(
                                                    cb.isNull(root.get("expiry")),
                                                    cb.greaterThan(root.get("expiry"), new Date()))))
                            .orderBy(cb.desc(root.get("updated"))));
    }

    @Override
    public boolean delete(String sessionId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredUserSession.class);
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
}
