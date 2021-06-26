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

import io.appform.idman.client.IdManClient;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.TokenInfo;
import io.appform.idman.model.TokenType;
import io.appform.idman.server.auth.TokenManager;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.Optional;

/**
 *
 */
@Slf4j
public class LocalIdmanClient extends IdManClient {

    private final AuthenticationConfig authConfig;
    private final TokenManager tokenManager;

    @Inject
    public LocalIdmanClient(AuthenticationConfig authConfig, TokenManager tokenManager) {
        this.authConfig = authConfig;
        this.tokenManager = tokenManager;
    }

    @Override
    public Optional<TokenInfo> accessToken(String serviceId, String tokenId) {
        return tokenManager.generateTokenForSession(serviceId, tokenId, TokenType.DYNAMIC)
                .map(generatedTokenInfo -> tokenInfo(generatedTokenInfo.getToken(), generatedTokenInfo.getUser()));
    }

    @Override
    protected Optional<TokenInfo> validateTokenImpl(String serviceId, String token) {
        return tokenManager.translateToken(serviceId, token)
                .map(user -> tokenInfo(token, user));
    }

    @Override
    public boolean deleteToken(String serviceId, String jwt) {
        return tokenManager.deleteToken(serviceId, jwt);
    }

    private TokenInfo tokenInfo(String token, IdmanUser user) {
        val expiry = authConfig.getMaxDynamicTokenRefreshInterval().toSeconds();
        return new TokenInfo(token, token, expiry, "bearer", user.getRole(), user);
    }
}
