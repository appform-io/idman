package io.appform.idman.authbundle.filters;


import com.google.common.base.Strings;
import io.appform.idman.authbundle.IdmanAuthenticationConfig;
import io.appform.idman.authbundle.security.ServiceUserPrincipal;
import io.dropwizard.auth.AuthFilter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

/**
 * This filter assigns role to validated user
 */
@Priority(Priorities.AUTHENTICATION)
@Slf4j
public class UserAuthorizationFilter extends AuthFilter<String, ServiceUserPrincipal> {

    private final IdmanAuthenticationConfig config;


    public UserAuthorizationFilter(IdmanAuthenticationConfig config) {
        this.config = config;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        val uriInfo = requestContext.getUriInfo();
        val requestURI = uriInfo.getPath(true);
        if (config.getAllowedPaths().stream().anyMatch(requestURI::startsWith)) {
            return;
        }
        val jwt = getTokenFromCookieOrHeader(requestContext).orElse(null);
        if (!this.authenticate(requestContext, jwt, "FORM")) {
/*            val uriBuilder = UriBuilder.fromUri(URI.create(config.getAuthEndpoint() + "/auth/login/" + config.getServiceId()));
            val serviceId = config.getServiceId();
            val referer = requestContext.getHeaders().getFirst("Referer");
            log.info("Referer: {}", referer);
            if(config.isServerMode()) {
                uriBuilder.queryParam("redirect",
                                      requestURI.replaceAll("^/apis", "")
                                        .replaceAll("^/ui", ""));
            }
            else {
                uriBuilder.queryParam("redirect", config.getRedirectionEndpoint() + requestURI);
            }
            val uri = uriBuilder.build();*/
            throw new WebApplicationException(Response.seeOther(
                    URI.create((!Strings.isNullOrEmpty(config.getResourcePrefix()) ? config.getResourcePrefix() : "")
                                       + "/idman/auth")).build());
        }
        log.debug("Auth successful");
    }

    private Optional<String> getTokenFromCookieOrHeader(final ContainerRequestContext requestContext) {
        val tokenFromHeader = getTokenFromHeader(requestContext);
        return tokenFromHeader.isPresent()
               ? tokenFromHeader
               : getTokenFromCookie(requestContext);
    }

    private Optional<String> getTokenFromHeader(final ContainerRequestContext requestContext) {
        val header = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            int space = header.indexOf(' ');
            if (space > 0) {
                final String method = header.substring(0, space);
                if ("Bearer".equalsIgnoreCase(method)) {
                    final String rawToken = header.substring(space + 1);
                    return Optional.of(rawToken);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> getTokenFromCookie(final ContainerRequestContext requestContext) {
        val idmanCookie = requestContext.getCookies().get("idman-token");
        if (null != idmanCookie) {
            return Optional.of(idmanCookie.getValue());
        }
        return Optional.empty();
    }

    public static class Builder extends AuthFilter.AuthFilterBuilder<String, ServiceUserPrincipal, UserAuthorizationFilter> {
        private final IdmanAuthenticationConfig authenticationConfig;

        public Builder(IdmanAuthenticationConfig authenticationConfig) {
            this.authenticationConfig = authenticationConfig;
        }

        protected UserAuthorizationFilter newInstance() {
            return new UserAuthorizationFilter(authenticationConfig);
        }
    }
}
