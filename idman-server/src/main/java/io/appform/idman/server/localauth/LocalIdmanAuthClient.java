package io.appform.idman.server.localauth;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.appform.idman.client.IdManClient;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.User;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.db.UserRoleStore;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.db.model.StoredUserRole;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 *
 */
@Slf4j
public class LocalIdmanAuthClient extends IdManClient {

    private final SessionStore sessionStore;
    private final UserInfoStore userInfoStore;
    private final ServiceStore serviceStore;
    private final UserRoleStore roleStore;
    private final AuthenticationConfig authConfig;


    private final LoadingCache<String, JwtConsumer> jwtConsumers;

    @Inject
    public LocalIdmanAuthClient(
            SessionStore sessionStore,
            UserInfoStore userInfoStore,
            ServiceStore serviceStore,
            UserRoleStore roleStore,
            AuthenticationConfig authConfig) {
        this.sessionStore = sessionStore;
        this.userInfoStore = userInfoStore;
        this.serviceStore = serviceStore;
        this.roleStore = roleStore;
        this.authConfig = authConfig;

        this.jwtConsumers = Caffeine.newBuilder()
                .build(serviceId -> buildConsumer(authConfig, serviceId));
    }

    @Override
    @UnitOfWork
    public IdmanUser validateImpl(String token, String serviceId) {
        log.debug("Auth called");
        val service = serviceStore.get(serviceId).orElse(null);
        if (null == service || service.isDeleted()) {
            log.warn("authentication_failed::invalid_service serviceId:{}", serviceId);
            return null;
        }
        final String userId;
        final String sessionId;
        final String extServiceId;
        try {
            val jwtConsumer = jwtConsumers.get(serviceId);
            Preconditions.checkNotNull(jwtConsumer, "No consumer found for service" + serviceId);

            val jwtContext = jwtConsumer.process(token);

            val claims = jwtContext.getJwtClaims();
            userId = claims.getSubject();
            sessionId = claims.getJwtId();
            extServiceId = claims.getAudience().get(0);
        }
        catch (MalformedClaimException | InvalidJwtException e) {
            log.error(String.format("exception in claim extraction %s", e.getMessage()), e);
            return null;
        }
        log.debug("authentication_requested userId:{} tokenId:{}", userId, sessionId);
        if(!extServiceId.equalsIgnoreCase(serviceId)) {
            log.warn("authentication_failed::service_id_mismatch userId:{} tokenId:{}", userId, sessionId);
            return null;
        }

        val session = sessionStore.get(sessionId).orElse(null);
        if (session == null) {
            log.warn("authentication_failed::invalid_session userId:{} tokenId:{}", userId, sessionId);
            return null;
        }
        if (!session.getUserId().equals(userId)) {
            log.warn("authentication_failed::user_mismatch userId:{} tokenId:{}", userId, sessionId);
            return null;
        }
        if(session.getExpiry() != null && session.getExpiry().before(new Date())) {
            log.warn("authentication_failed::session_expired userId:{} tokenId:{}", userId, sessionId);
            return null;
        }
        val user = userInfoStore.get(session.getUserId()).orElse(null);
        if (null == user || user.isDeleted()) {
            log.warn("authentication_failed::invalid_user userId:{} tokenId:{}", userId, sessionId);
            return null;
        }
        val role = roleStore.getUserServiceRole(userId, extServiceId)
                .map(StoredUserRole::getRoleId)
                .orElse(null);
        if (Strings.isNullOrEmpty(role)) {
            log.error("No valid role found for user: {} in service: {}", userId, extServiceId);
        }
        log.debug("authentication_success userId:{} tokenId:{}", userId, sessionId);
        return new IdmanUser(sessionId,
                             serviceId,
                             new User(user.getUserId(),
                                      user.getName(),
                                      user.getUserType(),
                                      user.getAuthState().getAuthMode()),
                             role);
    }

    private JwtConsumer buildConsumer(AuthenticationConfig authConfig, final String serviceId) {
        val jwtConfig = authConfig.getJwt();
        final byte[] secretKey = jwtConfig.getPrivateKey().getBytes(StandardCharsets.UTF_8);
        return new JwtConsumerBuilder()
                .setRequireIssuedAt()
                .setRequireSubject()
                .setExpectedIssuer(jwtConfig.getIssuerId())
                .setVerificationKey(new HmacKey(secretKey))
                .setJwsAlgorithmConstraints(new AlgorithmConstraints(
                        AlgorithmConstraints.ConstraintType.WHITELIST,
                        AlgorithmIdentifiers.HMAC_SHA512))
                .setExpectedAudience(serviceId)
                .build();
    }
}
