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

package io.appform.idman.server.localauth;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.appform.idman.client.IdManClient;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.TokenInfo;
import io.appform.idman.model.TokenType;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.db.UserRoleStore;
import io.appform.idman.server.db.model.ClientSession;
import io.appform.idman.server.db.model.StoredUserRole;
import io.appform.idman.server.utils.Utils;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jose4j.jwt.consumer.JwtConsumer;

import javax.inject.Inject;
import java.util.Optional;

import static io.appform.idman.server.utils.Utils.toWire;

/**
 *
 */
@Slf4j
public class LocalIdmanAuthClient extends IdManClient {

    private final SessionStore sessionStore;
    private final UserInfoStore userInfoStore;
    private final ServiceStore serviceStore;
    private final UserRoleStore roleStore;


    private final LoadingCache<String, JwtConsumer> jwtConsumers;
    private final AuthenticationConfig authConfig;

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

        this.jwtConsumers = Caffeine.newBuilder().build(serviceId -> Utils.buildConsumer(authConfig, serviceId));
        this.authConfig = authConfig;
    }

    @Override
    @UnitOfWork
    public Optional<TokenInfo> accessToken(String serviceId, String tokenId) {
        val session = sessionStore.get(tokenId, TokenType.DYNAMIC).orElse(null);
        if (null == session) {
            return Optional.empty();
        }
        if (!serviceId.equals(session.getServiceId())) {
            return Optional.empty();
        }
        val token = Utils.createAccessToken(session, authConfig.getJwt(), TokenType.DYNAMIC);
        val idmanUser = buildIdmanUser(session);
        return Optional.of(new TokenInfo(token,
                                         token,
                                         //Refresh token is same as token for us. We shall re-validate it every time refresh is called
                                         authConfig.getJwt().getMaxDynamicTokenLifetime().toSeconds(),
                                         "beader",
                                         idmanUser.getRole(),
                                         idmanUser));
    }

    @Override
    @UnitOfWork
    public Optional<TokenInfo> refreshAccessTokenImpl(String serviceId, String token) {
        val user = validateImpl(token, serviceId);
        if(null == user) {
            return Optional.empty();
        }
        return Optional.of(new TokenInfo(token,
                                         token,
                                         authConfig.getJwt().getMaxDynamicTokenLifetime().toSeconds(),
                                         "bearer",
                                         user.getRole(),
                                         user));
    }

    public IdmanUser validateImpl(String token, String serviceId) {
        log.debug("Auth called");
        val service = serviceStore.get(serviceId).orElse(null);
        if (null == service || service.isDeleted()) {
            log.warn("authentication_failed::invalid_service serviceId:{}", serviceId);
            return null;
        }
        val jwtConsumer = consumer(serviceId);
        val parsedToken = Utils.parseToken(token, jwtConsumer).orElse(null);
        if (null == parsedToken) {
            return null;
        }
        if (!parsedToken.getServiceId().equals(serviceId)) {
            log.error("authentication_failed::service id mismatch");
        }
        val session = sessionStore
                .get(parsedToken.getSessionId(),
                     parsedToken.getExpiry() != null
                     ? TokenType.DYNAMIC
                     : TokenType.STATIC)
                .orElse(null);
        if (session == null) {
            log.warn("authentication_failed::invalid_session userId:{} tokenId:{}",
                     parsedToken.getUserId(),
                     parsedToken.getSessionId());
            return null;
        }
        return buildIdmanUser(session);
    }

    private JwtConsumer consumer(String serviceId) {
        val jwtConsumer = jwtConsumers.get(serviceId);
        Preconditions.checkNotNull(jwtConsumer, "No consumer found for service" + serviceId);
        return jwtConsumer;
    }

    private IdmanUser buildIdmanUser(ClientSession session) {
        val userId = session.getUserId();
        val serviceId = session.getServiceId();
        val sessionId = session.getSessionId();
        val user = userInfoStore.get(session.getUserId()).orElse(null);
        if (null == user || user.isDeleted()) {
            log.warn("authentication_failed::invalid_user userId:{} tokenId:{}", userId, sessionId);
            return null;
        }
        val role = roleStore.getUserServiceRole(userId, serviceId)
                .map(StoredUserRole::getRoleId)
                .orElse(null);
        if (Strings.isNullOrEmpty(role)) {
            log.error("No valid role found for user: {} in service: {}", userId, serviceId);
        }
        log.debug("authentication_success userId:{} tokenId:{}", userId, sessionId);
        return new IdmanUser(sessionId,
                             serviceId,
                             toWire(user),
                             role);
    }

}
