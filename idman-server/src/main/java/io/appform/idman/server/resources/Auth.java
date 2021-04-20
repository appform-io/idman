package io.appform.idman.server.resources;

import io.appform.idman.authbundle.security.ServiceUserPrincipal;
import io.appform.idman.model.AuthMode;
import io.appform.idman.server.auth.AuthenticationProviderRegistry;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.impl.PasswordAuthInfo;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.utils.Utils;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.vyarus.guicey.gsp.views.template.Template;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 *
 */
@Slf4j
@Path("/ui/auth")
@Template
@Produces(MediaType.TEXT_HTML)
public class Auth {
    private static final String STATE_COOKIE_NAME = "oauth-state";

    private final AuthenticationConfig authenticationConfig;
    private final Provider<AuthenticationProviderRegistry> authenticators;
    private final Provider<SessionStore> sessionStore;

    @Inject
    public Auth(
            AuthenticationConfig authenticationConfig,
            Provider<AuthenticationProviderRegistry> authenticators,
            Provider<SessionStore> sessionStore) {
        this.authenticationConfig = authenticationConfig;
        this.authenticators = authenticators;
        this.sessionStore = sessionStore;
    }

    @Path("/login")
    @GET
    public Response loginScreen() {
        return Response.ok(new TemplateView("templates/loginscreen.hbs")).build();
    }

    @Path("/login/password")
    @POST
    @UnitOfWork
    public Response passwordLogin(
            @FormParam("email") @NotEmpty @Size(max = 255) final String email,
            @FormParam("password") @NotEmpty @Size(max = 255) final String password) {
        val authenticationProvider = authenticators.get()
                .provider(AuthMode.PASSWORD)
                .orElse(null);
        Objects.requireNonNull(authenticationProvider, "No authenticator found");
        val session = authenticationProvider.login(new PasswordAuthInfo(email, password), UUID.randomUUID().toString())
                .orElse(null);
        if(session == null) {
            return Response.seeOther(URI.create("/auth/login")).build();
        }
        return Response.seeOther(URI.create("/"))
                .cookie(new NewCookie("idman-token",
                                      Utils.createJWT(session, authenticationConfig.getJwt()),
                                      "/",
                                      authenticationConfig.getDomain(),
                                      Cookie.DEFAULT_VERSION,
                                      null,
                                      NewCookie.DEFAULT_MAX_AGE,
                                      session.getExpiry(),
                                      true,
                                      true))
                .build();
    }

    @Path("/logout")
    @POST
    @UnitOfWork
    @PermitAll
    public Response logout(
            @io.dropwizard.auth.Auth final ServiceUserPrincipal principal,
            @CookieParam("idman-token") final Cookie idmanToken) {
        val sessionId = principal.getServiceUser().getSessionId();
        val status = sessionStore.get().delete(sessionId);
        log.info("Session {} deletion status for user {}: {}",
                 sessionId, principal.getServiceUser().getUser().getId(), status);
        return Response.seeOther(URI.create("/"))
                .cookie(new NewCookie("idman-token",
                                      "",
                                      "/",
                                      authenticationConfig.getDomain(),
                                      Cookie.DEFAULT_VERSION,
                                      null,
                                      0,
                                      null,
                                      true,
                                      true))
                .build();

    }
}
