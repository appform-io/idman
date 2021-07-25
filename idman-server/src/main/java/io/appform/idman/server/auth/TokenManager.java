package io.appform.idman.server.auth;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Strings;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.TokenType;
import io.appform.idman.model.UserType;
import io.appform.idman.server.auth.configs.JwtConfig;
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.db.UserRoleStore;
import io.appform.idman.server.db.model.ClientSession;
import io.appform.idman.server.db.model.StoredUserRole;
import io.appform.idman.server.utils.Utils;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jose4j.jwt.consumer.JwtConsumer;

import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static io.appform.idman.server.utils.Utils.toWire;

/**
 *
 */
@Slf4j
public class TokenManager {
    @Value
    public static class GeneratedTokenInfo {
        IdmanUser user;
        String token;
    }

    private final UserInfoStore userInfoStore;
    private final ServiceStore serviceStore;
    private final SessionStore sessionStore;
    private final UserRoleStore roleStore;
    private final JwtConfig jwtConfig;
    private final LoadingCache<String, JwtConsumer> jwtConsumers;

    @Inject
    public TokenManager(
            UserInfoStore userInfoStore,
            ServiceStore serviceStore,
            SessionStore sessionStore,
            UserRoleStore roleStore,
            JwtConfig jwtConfig) {
        this.userInfoStore = userInfoStore;
        this.serviceStore = serviceStore;
        this.sessionStore = sessionStore;
        this.roleStore = roleStore;
        this.jwtConfig = jwtConfig;
        this.jwtConsumers = Caffeine.newBuilder().build(serviceId -> Utils.buildConsumer(jwtConfig, serviceId));
    }

    @UnitOfWork
    public Optional<ClientSession> createToken(
            String serviceId,
            String userId,
            String clientSessionId,
            TokenType type,
            Date expiry) {
        if ((type == TokenType.DYNAMIC && expiry == null)
            || (type == TokenType.STATIC && expiry != null)) {
            return Optional.empty();
        }
        val user = userInfoStore.get(userId).orElse(null);
        val service = serviceStore.get(serviceId).orElse(null);
        if (null == user || user.isDeleted()
                || (user.getUserType() == UserType.HUMAN && type == TokenType.STATIC)
                || (user.getUserType() == UserType.SYSTEM && type == TokenType.DYNAMIC)
                || null == service || service.isDeleted()) {
            return Optional.empty();
        }
        return sessionStore
                .create(UUID.randomUUID().toString(), userId, serviceId, clientSessionId, type, expiry);
    }

    @UnitOfWork
    public Optional<GeneratedTokenInfo> generateTokenForSession(String serviceId, String sessionId, TokenType tokenType) {
        return sessionStore.get(sessionId, tokenType)
                .filter(session -> session.getServiceId().equals(serviceId))
                .flatMap(session -> buildIdmanUser(session)
                        .map(user -> new GeneratedTokenInfo(user, Utils.createAccessToken(session, jwtConfig))));
    }

    @UnitOfWork
    public Optional<IdmanUser> translateToken(String serviceId, String token) {
        log.debug("Auth called");
        return serviceStore.get(serviceId)
                .filter(service -> !service.isDeleted())
                .flatMap(service -> parseToken(serviceId, token)
                        .map(parsedToken -> sessionStore.get(parsedToken.getSessionId(), parsedToken.getType())
                                .map(this::buildIdmanUser)
                                .orElseGet(() -> {
                                    log.warn("authentication_failed::invalid_session userId:{} tokenId:{}",
                                             parsedToken.getUserId(),
                                             parsedToken.getSessionId());
                                    return Optional.empty();
                                })))
                .orElseGet(() -> {
                    log.warn("authentication_failed::invalid_service serviceId:{}", serviceId);
                    return Optional.empty();
                });
    }

    @UnitOfWork
    public boolean deleteToken(String serviceId, String jwt) {
        return serviceStore.get(serviceId)
                .filter(service -> !service.isDeleted())
                .flatMap(service -> parseToken(serviceId, jwt))
                .map(parsedToken -> sessionStore.delete(parsedToken.getSessionId(), parsedToken.getType()))
                .orElse(false);
    }

    public Optional<ParsedTokenInfo> parseToken(String serviceId, String token) {
        return Optional.ofNullable(jwtConsumers.get(serviceId))
                .flatMap(consumer -> Utils.parseToken(token, consumer));
    }

    private Optional<IdmanUser> buildIdmanUser(ClientSession session) {
        val userId = session.getUserId();
        val serviceId = session.getServiceId();
        val sessionId = session.getSessionId();
        return userInfoStore.get(session.getUserId())
                .filter(user -> !user.isDeleted())
                .map(user -> {
                    val role = roleStore.getUserServiceRole(userId, serviceId)
                            .map(StoredUserRole::getRoleId)
                            .orElse(null);
                    if (Strings.isNullOrEmpty(role)) {
                        log.error("No valid role found for user: {} in service: {}", userId, serviceId);
                        return Optional.<IdmanUser>empty();
                    }
                    log.debug("authentication_success userId:{} tokenId:{}", userId, sessionId);
                    return Optional.of(new IdmanUser(sessionId, serviceId, toWire(user), role));
                })
                .orElseGet(() -> {
                    log.warn("authentication_failed::invalid_user userId:{} tokenId:{}", userId, sessionId);
                    return Optional.empty();
                });
    }
}
