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

package io.appform.idman.server.auth.impl;

import io.appform.idman.model.AuthMode;
import io.appform.idman.server.AuthenticatorContextVisitorAdapter;
import io.appform.idman.server.auth.*;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.db.PasswordStore;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.db.model.StoredUser;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Optional;

/**
 * Provides password based auth
 */
@Slf4j
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PasswordAuthenticationProvider extends AuthenticationProvider {

    private final Provider<UserInfoStore> userStore;
    private final Provider<PasswordStore> passwordStore;

    @Inject
    public PasswordAuthenticationProvider(
            AuthenticationConfig authConfig,
            Provider<UserInfoStore> userStore,
            Provider<PasswordStore> passwordStore,
            Provider<SessionStore> sessionStore) {
        super(AuthMode.PASSWORD, authConfig, userStore, sessionStore);
        this.userStore = userStore;
        this.passwordStore = passwordStore;
    }


    @Override
    public String redirectionURL(String sessionId) {
        throw new UnsupportedOperationException("This is not yet implemented");
    }

    @Override
    protected AuthenticatorContext createContext(AuthInfo authInfo) {
        return new PasswordAuthenticatorContext(toPwdAuthInfo(authInfo));
    }

    @Override
    protected final Optional<StoredUser> fetchUserDetails(AuthenticatorContext context) {
        val pwdCtx = pwdContext(context);
        return userStore.get().getByEmail(pwdCtx.getPwdAuthInfo().getEmail());
    }


    @Override
    protected final boolean authenticate(AuthenticatorContext context, StoredUser user) {
        val pwdCtx = pwdContext(context);
        return passwordStore.get().match(user.getUserId(), pwdCtx.getPwdAuthInfo().getPassword());
    }

    private PasswordAuthInfo toPwdAuthInfo(AuthInfo authInfo) {
        return authInfo.accept(new AuthInfoVisitor<PasswordAuthInfo>() {
            @Override
            public PasswordAuthInfo visit(PasswordAuthInfo passwordAuthInfo) {
                return passwordAuthInfo;
            }

            @Override
            public PasswordAuthInfo visit(GoogleAuthInfo googleAuthInfo) {
                throw new IllegalArgumentException("Google auth info passed to password authenticator");
            }
        });
    }

    private PasswordAuthenticatorContext pwdContext(AuthenticatorContext context) {
        return context.accept(new AuthenticatorContextVisitorAdapter<PasswordAuthenticatorContext>() {
            @Override
            public PasswordAuthenticatorContext visit(PasswordAuthenticatorContext passwordAuthenticatorContext) {
                return passwordAuthenticatorContext;
            }
        });
    }

}
