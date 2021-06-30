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

package io.appform.idman.authcomponents.resource;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.client.IdManClient;
import io.appform.idman.client.IdmanClientConfig;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.UUID;

/**
 *
 */
@Path("/idman/auth")
@Slf4j
public class IdmanAuthHandler {
    private static final String IDMAN_STATE_COOKIE_NAME = "idman-auth-state";
    private static final String IDMAN_LOCAL_REDIRECT = "idman-local-redirect";
    private static final String IDMAN_TOKEN_COOKIE_NAME = "idman-token";
    private static final String AUTH_API = "/idman/auth";

    private final IdManClient idManClient;
    private final IdmanClientConfig config;

    @Inject
    public IdmanAuthHandler(
            IdManClient idManClient,
            IdmanClientConfig config) {
        this.idManClient = idManClient;
        this.config = config;
    }

    @GET
    public Response startAuth(
            @HeaderParam("Referer") final String referrer,
            @CookieParam(IDMAN_LOCAL_REDIRECT) final Cookie localRedirect,
            @QueryParam("error") final String error) {
        val callbackPath = prefixedPath("/idman/auth/callback");
        val clientAuthSessionId = UUID.randomUUID().toString();
        val uriBuilder = UriBuilder.fromUri(config.getAuthEndpoint() + "/apis/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", config.getServiceId())
                .queryParam("redirect_uri", URI.create(callbackPath))
                .queryParam("state", clientAuthSessionId);
        if (!Strings.isNullOrEmpty(error)) {
            uriBuilder.queryParam("error", error);
        }
        val idmanUri = uriBuilder.build();
        val finalRedirect = null != localRedirect
                            ? localRedirect.getValue()
                            : referrer;
        log.debug("Final Redirect: {}", finalRedirect);
        return Response.seeOther(idmanUri)
                .cookie(new NewCookie(
                        IDMAN_LOCAL_REDIRECT,
                        finalRedirect,
                        callbackPath,
                        null,
                        Cookie.DEFAULT_VERSION,
                        null,
                        NewCookie.DEFAULT_MAX_AGE,
                        null,
                        false,
                        true))
                .cookie(new NewCookie(
                        IDMAN_STATE_COOKIE_NAME,
                        clientAuthSessionId,
                        callbackPath,
                        null,
                        Cookie.DEFAULT_VERSION,
                        null,
                        NewCookie.DEFAULT_MAX_AGE,
                        null,
                        false,
                        true))
                .build();
    }

    @GET
    @Path("/callback")
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleCallback(
            @CookieParam(IDMAN_STATE_COOKIE_NAME) final Cookie cookieState,
            @CookieParam(IDMAN_LOCAL_REDIRECT) final Cookie localRedirect,
            @QueryParam("state") final String clientSessionId,
            @QueryParam("code") final String code,
            @QueryParam("error") final String errorCode,
            @QueryParam("error_description") final String errorDescription) {
        if (!Strings.isNullOrEmpty(errorCode)) {
            return Response.ok(
                    ImmutableMap.of(
                            "errorCode", errorCode,
                            "errorDescription", errorDescription))
                    .build();
        }
        if (null == cookieState || null == localRedirect) {
            log.error("Missing cookie params for callback");
            return Response.seeOther(URI.create(prefixedPath(AUTH_API))).build();
        }
        if (!cookieState.getValue().equals(clientSessionId)) {
            log.error("State cookie mismatch. Expected: {} Received callback for: {}",
                      cookieState.getValue(), clientSessionId);
            return Response.seeOther(URI.create(prefixedPath(AUTH_API))).build();
        }
        val tokenInfo = idManClient.accessToken(config.getServiceId(), code).orElse(null);
        if (null == tokenInfo) {
            log.error("Token validation failed. Token: {}", code);
            return Response.seeOther(URI.create(prefixedPath(AUTH_API))).build();
        }

        val localRedirectPath = Strings.isNullOrEmpty(localRedirect.getValue())
                                ? config.getPublicEndpoint()
                                : localRedirect.getValue();
        log.debug("Redirecting to: {}. Local redirect: {}", localRedirectPath, localRedirect.getValue());
        return Response.seeOther(URI.create(localRedirectPath))
                .cookie(new NewCookie(cookieName(),
                                      tokenInfo.getAccessToken(),
                                      "/",
                                      null,
                                      Cookie.DEFAULT_VERSION,
                                      null,
                                      NewCookie.DEFAULT_MAX_AGE,
                                      null,
                                      false,
                                      true),
                        expireCookie(cookieState),
                        expireCookie(localRedirect))
                .build();
    }

    @Path("/logout")
    @POST
    @PermitAll
    public Response logout(@Auth final ServiceUserPrincipal principal) {
        val sessionId = principal.getServiceUser().getSessionId();
        log.debug("Logging out session: {}", sessionId);
//        TODO::INTRODUCE CLIENT LEVEL LOGOUT val status = sessionStore.get().delete(sessionId);
//        log.info("Session {} deletion status for user {}: {}",
//                 sessionId, principal.getServiceUser().getUser().getId(), status);
        return Response.seeOther(URI.create(config.getPublicEndpoint()))
                .cookie(new NewCookie(cookieName(),
                                      "",
                                      "/",
                                      null,
                                      Cookie.DEFAULT_VERSION,
                                      null,
                                      NewCookie.DEFAULT_MAX_AGE,
                                      null,
                                      false,
                                      true))
                .build();

    }

    private String prefixedPath(String path) {
        return config.getPublicEndpoint()
                + ((!Strings.isNullOrEmpty(config.getResourcePrefix())
                    ? config.getResourcePrefix()
                    : "") + path);
    }

    private String cookieName() {
        return IDMAN_TOKEN_COOKIE_NAME + "-" + config.getServiceId().toLowerCase();
    }


    private NewCookie expireCookie(Cookie cookie) {
        return new NewCookie(cookie, null, 0, null, false, true);
    }

}
