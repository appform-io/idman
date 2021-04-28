package io.appform.idman.authbundle;

import io.dropwizard.auth.UnauthorizedHandler;

import javax.ws.rs.core.Response;
import java.net.URI;

/**
 *
 */
public class RedirectUnauthorizedHandler implements UnauthorizedHandler {
    private final IdmanAuthenticationConfig authenticationConfig;

    public RedirectUnauthorizedHandler(IdmanAuthenticationConfig authenticationConfig) {
        this.authenticationConfig = authenticationConfig;
    }

    @Override
    public Response buildResponse(String prefix, String realm) {
        return Response.seeOther(URI.create(authenticationConfig.getAuthEndpoint())).build();
    }
}
