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
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.db.UserRoleStore;
import io.appform.idman.server.db.model.StoredUserRole;
import io.appform.idman.server.utils.Utils;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;

import javax.inject.Inject;

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
            log.error("exception in claim extraction {}. Token: {}", e.getMessage(), token);
            return null;
        }

        val session = sessionStore.get(sessionId).orElse(null);
        if (session == null) {
            log.warn("authentication_failed::invalid_session userId:{} tokenId:{}", userId, sessionId);
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
                             toWire(user),
                             role);
    }

}
