package io.appform.idman.server.resources;

import com.google.common.base.Strings;
import io.appform.idman.model.AuthMode;
import io.appform.idman.server.auth.AuthenticationProviderRegistry;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.impl.GoogleAuthInfo;
import io.appform.idman.server.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.UUID;

/**
 * Oauth Apis
 */
@Path("/oauth")
@Produces(MediaType.TEXT_HTML)
@Slf4j
public class OAuth {
    private static final String STATE_COOKIE_NAME = "oauth-state";

    private final AuthenticationConfig authConfig;
    private final Provider<AuthenticationProviderRegistry> authProviderRegistry;

    @Inject
    public OAuth(AuthenticationConfig authConfig, Provider<AuthenticationProviderRegistry> authProviderRegistry) {
        this.authConfig = authConfig;
        this.authProviderRegistry = authProviderRegistry;
    }

    @GET
    @Path("login/{provider}")
    public Response login(
            @PathParam("provider") final AuthMode providerType,
            @CookieParam("redirection") final Cookie cookieReferrer,
            @HeaderParam("Referer") final String referrer) {
        val authProvider = authProviderRegistry.get().provider(providerType).orElse(null);
        if(null == authProvider) {
            log.warn("No provider found for type: {}", providerType.name());
            return Response.seeOther(URI.create("/login")).build();
        }
        final String sessionId = UUID.randomUUID().toString();
        final String redirectionURL = authProvider.redirectionURL(sessionId);
        log.debug("Redirection uri: {}", redirectionURL);
        final String cookieReferrerUrl = null == cookieReferrer ? null : cookieReferrer.getValue();
        val source = Strings.isNullOrEmpty(cookieReferrerUrl) ? referrer : cookieReferrerUrl;
        log.debug("Call source: {} Referrer: {} Redirection: {}", source, referrer, cookieReferrerUrl);
        if(!Strings.isNullOrEmpty(source)) {
            log.debug("Saved: {} against session: {}", source, sessionId);
        }
        return Response.seeOther(URI.create(redirectionURL))
                .cookie(new NewCookie(
                        STATE_COOKIE_NAME,
                        sessionId,
                        "/oauth/callback/"+ providerType.name(),
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
    @Path("/callback/{provider}")
    public Response handleGoogleCallback(
            @PathParam("provider") final AuthMode providerType,
            @CookieParam(STATE_COOKIE_NAME) final Cookie cookieState,
            @Context HttpServletRequest requestContext,
            @QueryParam("state") final String sessionId,
            @QueryParam("code") final String authCode) {
        log.info("Request Ctx: {}", requestContext);
        val authProvider = authProviderRegistry.get().provider(providerType).orElse(null);
        if(null == authProvider) {
            log.warn("No provider found for type: {}", providerType.name());
            return seeOther(cookieState);
        }
        if (null == cookieState
                || !cookieState.getValue().equals(sessionId)) {
            return seeOther(cookieState);
        }
        val token = authProvider.login(new GoogleAuthInfo(authCode), sessionId).orElse(null);
        if (null == token) {
            log.debug("No token returned by provider login.");
            return seeOther(cookieState);
        }
        //TODO::SUBDOMAIN COOKIE
        return Response.seeOther(URI.create("/"))
                .cookie(new NewCookie("token",
                                      Utils.createJWT(token, authConfig.getJwt()),
                                      "/",
                                      authConfig.getDomain(),
                                      Cookie.DEFAULT_VERSION,
                                      null,
                                      NewCookie.DEFAULT_MAX_AGE,
                                      null,
                                      true,
                                      true),
                        new NewCookie(cookieState, null, 0, false))
                .build();
    }

    private Response seeOther(final Cookie cookieState) {
        return Response.seeOther(URI.create("/"))
                .cookie(new NewCookie(cookieState, null, 0, false))
                .build();
    }
}
