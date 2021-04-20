package io.appform.idman.server.db;

import com.google.common.base.Strings;
import io.appform.idman.client.IdManClient;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.User;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.db.model.StoredServiceUserRole;
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
import java.util.Optional;

/**
 *
 */
@Slf4j
public class LocalIdmanAuthClient implements IdManClient {

    private final SessionStore sessionStore;
    private final UserInfoStore userInfoStore;
    private final ServiceStore serviceStore;
    private final ServiceUserRoleStore roleStore;
    private final AuthenticationConfig authConfig;


    private final JwtConsumer jwtConsumer;

    @Inject
    public LocalIdmanAuthClient(
            SessionStore sessionStore,
            UserInfoStore userInfoStore,
            ServiceStore serviceStore,
            ServiceUserRoleStore roleStore,
            AuthenticationConfig authConfig) {
        this.sessionStore = sessionStore;
        this.userInfoStore = userInfoStore;
        this.serviceStore = serviceStore;
        this.roleStore = roleStore;
        this.authConfig = authConfig;

        this.jwtConsumer = buildConsumer(this.authConfig);
    }

    @Override
    @UnitOfWork
    public Optional<IdmanUser> validate(String serviceId, String token) {
        val service = serviceStore.get(serviceId).orElse(null);
        if (null == service || service.isDeleted()) {
            log.warn("Invalid service {} for token {}", serviceId, token);
            return Optional.empty();
        }
        log.debug("Auth called");
        final String userId;
        final String sessionId;
        final String serviceName;
        try {
            val jwtContext = jwtConsumer.process(token);

            val claims = jwtContext.getJwtClaims();
            userId = claims.getSubject();
            sessionId = claims.getJwtId();
            serviceName = claims.getAudience().get(0);
        }
        catch (MalformedClaimException | InvalidJwtException e) {
            log.error(String.format("exception in claim extraction %s", e.getMessage()), e);
            return Optional.empty();
        }
        log.debug("authentication_requested userId:{} tokenId:{}", userId, sessionId);
        val session = sessionStore.get(sessionId).orElse(null);
        if (session == null || session.isDeleted()) {
            log.warn("authentication_failed::invalid_session userId:{} tokenId:{}", userId, sessionId);
            return Optional.empty();
        }
        if (!session.getUserId().equals(userId)) {
            log.warn("authentication_failed::user_mismatch userId:{} tokenId:{}", userId, sessionId);
            return Optional.empty();
        }
        val user = userInfoStore.get(session.getUserId()).orElse(null);
        if (null == user || user.isDeleted()) {
            log.warn("authentication_failed::invalid_user userId:{} tokenId:{}", userId, sessionId);
            return Optional.empty();
        }
        val expectedServiceName = authConfig.getJwt().getDomain();
        if (!serviceName.equals(expectedServiceName)) {
            log.warn("authentication_failed::invalid_audience audience provided: {} userid: {} expected: {}",
                     serviceName, userId, expectedServiceName);
            return Optional.empty();
        }
        val role = roleStore.getUserServiceRole(userId, serviceId)
                .map(StoredServiceUserRole::getRoleId)
                .orElse(null);
        if (Strings.isNullOrEmpty(role)) {
            log.error("No valid role found for user: {} in service: {}", userId, serviceId);
        }
        log.debug("authentication_success userId:{} tokenId:{}", userId, sessionId);
        return Optional.of(new IdmanUser(sessionId,
                                         new User(user.getUserId(),
                                                           user.getName(),
                                                           user.getUserType(),
                                                           user.getAuthState().getAuthMode()),
                                         role));
    }

    @Override
    public Optional<IdmanUser> getUserInfo(String serviceId, String userId) {
        return Optional.empty();
    }

    private JwtConsumer buildConsumer(AuthenticationConfig authConfig) {
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
                .setExpectedAudience(jwtConfig.getDomain())
                .build();
    }
}
