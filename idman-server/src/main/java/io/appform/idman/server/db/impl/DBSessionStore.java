package io.appform.idman.server.db.impl;

import io.appform.idman.server.db.FieldNames;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.model.SessionType;
import io.appform.idman.server.db.model.StoredUserSession;
import io.dropwizard.hibernate.AbstractDAO;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
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
            return get(sessionId);
        }
    }

    @Override
    @SneakyThrows
    public Optional<StoredUserSession> get(String sessionId) {
        val cb = currentSession().getCriteriaBuilder();
        val cr = criteriaQuery();
        val root = cr.from(StoredUserSession.class);
        return list(cr.select(root).where(cb.equal(root.get("sessionId"), sessionId)))
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
                                                    cb.isNotNull(root.get("expiry")),
                                                    cb.greaterThan(root.get("expiry").as(Date.class),
                                                                   cb.literal(new Date()))
                                                 ))).orderBy(cb.desc(root.get("updated"))));
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
