package io.appform.idman.authcomponents.resource;

import com.google.common.base.Strings;
import io.appform.idman.client.IdmanClientConfig;
import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.client.IdManClient;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
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

    private final IdManClient idManClient;
    private final IdmanClientConfig config;

    @Inject
    public IdmanAuthHandler(IdManClient idManClient, IdmanClientConfig config) {
        this.idManClient = idManClient;
        this.config = config;
    }

    @GET
    public Response startAuth(@HeaderParam("Referer") final String referrer,
                              @CookieParam(IDMAN_LOCAL_REDIRECT) final Cookie localRedirect,
                              @QueryParam("error") final String error) {
        val callbackPath = prexifedPath("/idman/auth/callback");
        val clientAuthSessionId = UUID.randomUUID().toString();
        val uriBuilder = UriBuilder.fromUri(config.getAuthEndpoint() + "/auth/login/" + config.getServiceId())
                .queryParam("redirect", callbackPath)
                .queryParam("clientSessionId", clientAuthSessionId);
        if(!Strings.isNullOrEmpty(error)) {
            uriBuilder.queryParam("error", Strings.isNullOrEmpty(error) ? "" : error);
        }
        val idmanUri = uriBuilder.build();
        val finalRedirect = null != localRedirect ? localRedirect.getValue() : referrer;
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
                        false))
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
                        false))
                .build();
    }

    @GET
    @Path("/callback")
    public Response handleCallback(
            @CookieParam(IDMAN_STATE_COOKIE_NAME) final Cookie cookieState,
            @CookieParam(IDMAN_LOCAL_REDIRECT) final Cookie localRedirect,
            @QueryParam("clientSessionId") final String clientSessionId,
            @QueryParam("code") final String token) {
        if (!cookieState.getValue().equals(clientSessionId)) {
            log.error("State cookie mismatch. Expected: {} Received callback for: {}",
                      cookieState.getValue(), clientSessionId);
            return Response.seeOther(URI.create(prexifedPath("/idman/auth"))).build();
        }
        val sessionUser = idManClient.validate(token, config.getServiceId()).orElse(null);
        if (null == sessionUser) {
            log.error("Token validation failed. Token: {}", token);
            return Response.seeOther(URI.create(prexifedPath("/idman/auth"))).build();
        }
        val localRedirectPath = Strings.isNullOrEmpty(localRedirect.getValue())
                                ? "/"
                                : localRedirect.getValue();
        log.debug("Redirecting to: {}. Local redirect: {}", localRedirectPath, localRedirect.getValue());
        return Response.seeOther(URI.create(localRedirectPath))
                .cookie(new NewCookie(cookieName(),
                                      token,
                                      "/",
                                      null,
                                      Cookie.DEFAULT_VERSION,
                                      null,
                                      NewCookie.DEFAULT_MAX_AGE,
                                      null,
                                      false,
                                      true),
                        new NewCookie(cookieState, null, 0, false),
                        new NewCookie(localRedirect, null, 0, false))
                .build();
    }

    @Path("/logout")
    @POST
    @PermitAll
    public Response logout(
            @io.dropwizard.auth.Auth final ServiceUserPrincipal principal,
            @CookieParam("idman-token") final Cookie idmanToken) {
        val sessionId = principal.getServiceUser().getSessionId();
//        TODO::INTRODUCE CLIENT LEVEL LOGOUT val status = sessionStore.get().delete(sessionId);
//        log.info("Session {} deletion status for user {}: {}",
//                 sessionId, principal.getServiceUser().getUser().getId(), status);
        return Response.seeOther(URI.create("/"))
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

    private String prexifedPath(String path) {
        return (!Strings.isNullOrEmpty(config.getResourcePrefix())
                ? config.getResourcePrefix()
                : "") + path;
    }

    private String cookieName() {
        return IDMAN_TOKEN_COOKIE_NAME + "-" + config.getServiceId().toLowerCase();
    }
}
