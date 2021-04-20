package io.appform.idman.server.db;

import io.appform.idman.server.db.model.SessionType;
import io.appform.idman.server.db.model.StoredUserSession;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * For session management
 */
public interface SessionStore {
    Optional<StoredUserSession> create(String sessionId, String userId, SessionType type, Date expiry);
    Optional<StoredUserSession> get(String sessionId);
    List<StoredUserSession> sessionsForUser(String userId);
    boolean delete(String sessionId);
}
