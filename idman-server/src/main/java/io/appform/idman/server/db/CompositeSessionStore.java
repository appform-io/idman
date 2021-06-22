package io.appform.idman.server.db;

import com.google.common.collect.ImmutableMap;
import io.appform.idman.server.db.model.ClientSession;
import io.appform.idman.model.TokenType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
@Singleton
public class CompositeSessionStore implements SessionStore {
    private Map<TokenType, SessionStoreForType> stores;

    @Inject
    public CompositeSessionStore(
            @Named("dynamic") final SessionStoreForType dynamicStore,
            @Named("static")  final SessionStoreForType staticStore) {
        stores = ImmutableMap.of(TokenType.DYNAMIC, dynamicStore, TokenType.STATIC, staticStore);
    }
    @Override
    public Optional<ClientSession> create(
            String sessionId, String userId, String serviceId, String clientSessionId, TokenType type, Date expiry) {
        return stores.get(type).create(sessionId, userId, serviceId, clientSessionId, expiry);
    }

    @Override
    public Optional<ClientSession> get(String sessionId, TokenType type) {
        return stores.get(type).get(sessionId);
    }

    @Override
    public List<ClientSession> sessionsForUser(String userId, TokenType type) {
        return stores.get(stores).sessionsForUser(userId);
    }

    @Override
    public boolean delete(String sessionId, TokenType type) {
        return stores.get(type).delete(sessionId);
    }
}
