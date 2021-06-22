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

package io.appform.idman.server.auth;

import io.appform.idman.model.AuthMode;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.db.AuthState;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.db.model.ClientSession;
import io.appform.idman.model.TokenType;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.utils.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Provider;
import java.util.Date;
import java.util.Optional;

/**
 *
 */
@Data
@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AuthenticationProvider {
    private static final int MAX_FAILURES = 3;

    @EqualsAndHashCode.Include
    private final AuthMode authMode;
    private final AuthenticationConfig authConfig;
    private final Provider<UserInfoStore> userStore;
    private final Provider<SessionStore> sessionStore;

    protected AuthenticationProvider(
            AuthMode authMode,
            AuthenticationConfig authConfig,
            Provider<UserInfoStore> userStore,
            Provider<SessionStore> sessionStore) {
        this.authMode = authMode;
        this.authConfig = authConfig;
        this.userStore = userStore;
        this.sessionStore = sessionStore;
    }

    public abstract String redirectionURL(String sessionId);

    public final Optional<ClientSession> login(final AuthInfo authInfo, String sessionId) {
        val context = createContext(authInfo);
        val user = fetchUserDetails(context).orElse(null);
        if (user == null
                || user.isDeleted()
                || user.getAuthState().getAuthState().equals(AuthState.LOCKED)) {
            return Optional.empty();
        }
        val userId = user.getUserId();

        if (!authenticate(context, user)) {
            userStore.get().updateAuthState(userId, authState -> {
                authState.setFailedAuthCount(authState.getFailedAuthCount() + 1);
                if (authState.getFailedAuthCount() >= MAX_FAILURES) {
                    authState.setAuthState(AuthState.LOCKED);
                }
            });
            log.warn("Authentication failure for: {}", userId);
            return Optional.empty();
        }
        else {
            userStore.get().updateAuthState(userId, authState -> authState.setFailedAuthCount(0));
        }
        val sessionDuration = Utils.sessionDuration(authConfig);
        return sessionStore.get().create(
                sessionId,
                user.getUserId(),
                authInfo.getServiceId(),
                authInfo.getClientSessionId(),
                TokenType.DYNAMIC, new Date(new Date().getTime() + sessionDuration.toMilliseconds()));
    }

    protected abstract AuthenticatorContext createContext(final AuthInfo authInfo);

    protected abstract Optional<StoredUser> fetchUserDetails(final AuthenticatorContext context);

    protected abstract boolean authenticate(final AuthenticatorContext context, final StoredUser user);
}
