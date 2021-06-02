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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import io.appform.idman.model.AuthMode;
import io.appform.idman.server.AuthenticatorContextVisitorAdapter;
import io.appform.idman.server.auth.*;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.configs.AuthenticationProviderConfigVisitor;
import io.appform.idman.server.auth.configs.CredentialAuthenticationProviderConfig;
import io.appform.idman.server.auth.configs.GoogleAuthenticationProviderConfig;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.utils.Utils;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Optional;

/**
 *
 */
@Slf4j
@Singleton
@EqualsAndHashCode(callSuper = true)
public class GoogleAuthenticationProvider extends AuthenticationProvider {

    private final HttpTransport transport;
    private final GoogleAuthorizationCodeFlow authorizationCodeFlow;
    private final String redirectionUrl;
    private final AuthenticationConfig authConfig;
    private final ObjectMapper mapper;
    private final Provider<UserInfoStore> userInfoStore;

    @Inject
    public GoogleAuthenticationProvider(
            AuthenticationConfig authConfig,
            GoogleAuthenticationProviderConfig googleAuthConfig,
            ObjectMapper mapper,
            Provider<UserInfoStore> userInfoStore,
            Provider<SessionStore> sessionStore) {
        super(AuthMode.GOOGLE_AUTH, authConfig, userInfoStore, sessionStore);
        this.authConfig = authConfig;
        this.userInfoStore = userInfoStore;
        final NetHttpTransport.Builder transportBuilder = new NetHttpTransport.Builder();
        Proxy proxy = Proxy.NO_PROXY;
        if (googleAuthConfig.getProxyType() != null) {
            switch (googleAuthConfig.getProxyType()) {
                case DIRECT:
                    break;
                case HTTP: {
                    Preconditions.checkArgument(!Strings.isNullOrEmpty(googleAuthConfig.getProxyHost()));
                    proxy = new Proxy(Proxy.Type.HTTP,
                                      new InetSocketAddress(googleAuthConfig.getProxyHost(),
                                                            googleAuthConfig.getProxyPort()));
                    break;
                }
                case SOCKS:
                    Preconditions.checkArgument(!Strings.isNullOrEmpty(googleAuthConfig.getProxyHost()));
                    proxy = new Proxy(Proxy.Type.HTTP,
                                      new InetSocketAddress(googleAuthConfig.getProxyHost(),
                                                            googleAuthConfig.getProxyPort()));
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + googleAuthConfig.getProxyType());
            }
        }
        this.transport = transportBuilder.setProxy(proxy)
                .build();
        this.authorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(
                transport,
                new JacksonFactory(),
                googleAuthConfig.getClientId(),
                googleAuthConfig.getClientSecret(),
                ImmutableSet.of("https://www.googleapis.com/auth/userinfo.email"))
                .build();
        this.redirectionUrl = Utils.redirectionUrl(AuthMode.GOOGLE_AUTH, authConfig);
        this.mapper = mapper;
    }

    @Override
    public String redirectionURL(String sessionId) {
        final String url = authorizationCodeFlow.newAuthorizationUrl()
                .setState(sessionId)
                .setRedirectUri(this.redirectionUrl)
//                .setRedirectUri("http://localhost:8080/auth/google")
                .build();
        val googleAuthConfig = authConfig.getProvider()
                .accept(new AuthenticationProviderConfigVisitor<GoogleAuthenticationProviderConfig>() {
                    @Override
                    public GoogleAuthenticationProviderConfig visit(
                            CredentialAuthenticationProviderConfig credentialAuthenticationProviderConfig) {
                        throw new IllegalArgumentException("No config found for google auth");
                    }

                    @Override
                    public GoogleAuthenticationProviderConfig visit(
                            GoogleAuthenticationProviderConfig googleAuthenticationConfig) {
                        return googleAuthenticationConfig;
                    }
                });
        return !Strings.isNullOrEmpty(googleAuthConfig.getLoginDomain())
               ? (url + "&hd=" + googleAuthConfig.getLoginDomain())
               : url;
    }

    @Override
    protected final AuthenticatorContext createContext(AuthInfo authInfo) {
        val authToken = authInfo.accept(new AuthInfoVisitor<String>() {
            @Override
            public String visit(PasswordAuthInfo passwordAuthInfo) {
                throw new IllegalStateException("Password passed to google auth provider");
            }

            @Override
            public String visit(GoogleAuthInfo googleAuthInfo) {
                return googleAuthInfo.getAuthToken();
            }
        });
        return new GoogleAuthenticatorContext(authToken);
    }

    @Override
    protected final Optional<StoredUser> fetchUserDetails(AuthenticatorContext context) {
        val gCtx = toGoogCtx(context);
        val authToken = gCtx.getAuthToken();
        if (Strings.isNullOrEmpty(authToken)) {
            return Optional.empty();
        }
        val authRequest = authorizationCodeFlow.newTokenRequest(authToken);
        final String email;
        try {
            final GoogleTokenResponse tokenResponse = authRequest
                    .setRedirectUri(this.redirectionUrl)
                    .execute();
            final Credential credential = authorizationCodeFlow.createAndStoreCredential(tokenResponse, null);
            final HttpRequestFactory requestFactory = transport.createRequestFactory(credential);
            // Make an authenticated request
            final GenericUrl url = new GenericUrl("https://www.googleapis.com/oauth2/v1/userinfo");
            final HttpRequest request = requestFactory.buildGetRequest(url);
            request.getHeaders().setContentType("application/json");
            val identity = mapper.readTree(request.execute().parseAsString());
            log.debug("Identity: {}", identity);
            email = identity.get("email").asText();
            gCtx.setIdentity(identity);
            gCtx.setEmail(email);
        }
        catch (IOException e) {
            log.error("Error logging in using google:", e);
            return Optional.empty();
        }
        return userInfoStore.get().getByEmail(email);
    }

    @Override
    protected final boolean authenticate(AuthenticatorContext context, StoredUser user) {
        val gCtx = toGoogCtx(context);
        return user.getEmail().equalsIgnoreCase(gCtx.getEmail());
    }

    private GoogleAuthenticatorContext toGoogCtx(final AuthenticatorContext context) {
        return context.accept(new AuthenticatorContextVisitorAdapter<GoogleAuthenticatorContext>() {

            @Override
            public GoogleAuthenticatorContext visit(GoogleAuthenticatorContext googleAuthenticatorContext) {
                return googleAuthenticatorContext;
            }
        });
    }
}
