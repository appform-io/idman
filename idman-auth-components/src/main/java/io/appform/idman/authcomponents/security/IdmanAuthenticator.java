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

package io.appform.idman.authcomponents.security;

import io.appform.idman.client.IdManClient;
import io.appform.idman.client.IdmanClientConfig;
import io.appform.idman.model.TokenInfo;
import io.dropwizard.auth.Authenticator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

/**
 * Authenticates token by calling server.
 */
@Slf4j
@Singleton
public class IdmanAuthenticator implements Authenticator<String, ServiceUserPrincipal> {

    private final IdmanClientConfig config;
    private final IdManClient idManClient;

    @Inject
    public IdmanAuthenticator(IdmanClientConfig config, IdManClient idManClient) {
        this.config = config;
        this.idManClient = idManClient;
    }

    @Override
    public Optional<ServiceUserPrincipal> authenticate(String token) {
        val serviceSessionUser
                = idManClient
                .refreshAccessToken(config.getServiceId(), token)
                .map(TokenInfo::getUser)
                .orElse(null);
        if (serviceSessionUser == null) {
            log.warn("authentication_failed::invalid_session token:{}", token);
            return Optional.empty();
        }
        log.debug("authentication_success userId:{} tokenId:{}",
                  serviceSessionUser.getUser().getId(), serviceSessionUser.getSessionId());
        return Optional.of(new ServiceUserPrincipal(serviceSessionUser));
    }
}
