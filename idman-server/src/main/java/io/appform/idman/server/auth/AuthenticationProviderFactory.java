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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.idman.server.auth.configs.*;
import io.appform.idman.server.auth.impl.GoogleAuthenticationProvider;
import io.appform.idman.server.db.PasswordStore;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.auth.impl.PasswordAuthenticationProvider;

import javax.inject.Provider;

/**
 * Creates auth provider based on config
 */
public class AuthenticationProviderFactory implements AuthenticationProviderConfigVisitor<AuthenticationProvider> {

    private final AuthenticationConfig authConfig;
    private final ObjectMapper mapper;
    private final Provider<UserInfoStore> userStore;
    private final Provider<SessionStore> sessionStore;
    private final Provider<PasswordStore> passwordStore;

    public AuthenticationProviderFactory(
            AuthenticationConfig authConfig,
            ObjectMapper mapper,
            Provider<UserInfoStore> userStore,
            Provider<SessionStore> sessionStore,
            Provider<PasswordStore> passwordStore) {
        this.authConfig = authConfig;
        this.mapper = mapper;
        this.userStore = userStore;
        this.sessionStore = sessionStore;
        this.passwordStore = passwordStore;
    }

    public AuthenticationProvider create(AuthenticationProviderConfig providerConfig) {
        return providerConfig.accept(this);
    }

    @Override
    public AuthenticationProvider visit(CredentialAuthenticationProviderConfig credentialAuthenticationProviderConfig) {
        return new PasswordAuthenticationProvider(authConfig, userStore, passwordStore, sessionStore);
    }

    @Override
    public AuthenticationProvider visit(GoogleAuthenticationProviderConfig googleAuthenticationConfig) {
        return new GoogleAuthenticationProvider(
                authConfig, googleAuthenticationConfig, mapper, userStore, sessionStore);
    }
}
