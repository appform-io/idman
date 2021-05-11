package io.appform.idman.authcomponents.security;

import io.appform.idman.client.IdmanClientConfig;
import io.dropwizard.auth.UnauthorizedHandler;

import javax.ws.rs.core.Response;
import java.net.URI;

/**
 *
 */
public class RedirectUnauthorizedHandler implements UnauthorizedHandler {
    private final IdmanClientConfig authenticationConfig;

    public RedirectUnauthorizedHandler(IdmanClientConfig authenticationConfig) {
        this.authenticationConfig = authenticationConfig;
    }

    @Override
    public Response buildResponse(String prefix, String realm) {
        return Response.seeOther(URI.create(authenticationConfig.getAuthEndpoint())).build();
    }
}
