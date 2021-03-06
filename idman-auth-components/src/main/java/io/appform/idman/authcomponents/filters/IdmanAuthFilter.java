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

package io.appform.idman.authcomponents.filters;


import com.google.common.base.Strings;
import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.client.IdmanClientConfig;
import io.dropwizard.auth.AuthFilter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Optional;

/**
 * This filter assigns role to validated user
 */
@Priority(Priorities.AUTHENTICATION)
@Slf4j
public class IdmanAuthFilter extends AuthFilter<String, ServiceUserPrincipal> {

    private static final String ERROR_PARAM = "error";
    private final IdmanClientConfig config;


    public IdmanAuthFilter(IdmanClientConfig config) {
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
            val params = uriInfo.getQueryParameters(true);
            val uriBuilder = UriBuilder.fromUri(URI.create(
                    config.getPublicEndpoint() +
                    (!Strings.isNullOrEmpty(config.getResourcePrefix())
                     ? config.getResourcePrefix() : "") + "/idman/auth"));
            if(params.containsKey(ERROR_PARAM)) {
                uriBuilder.queryParam(ERROR_PARAM, params.getFirst(ERROR_PARAM));
            }
            throw new WebApplicationException(Response.seeOther(uriBuilder.build()).build());
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
        val idmanCookie = requestContext.getCookies().get("idman-token-" + config.getServiceId().toLowerCase());
        if (null != idmanCookie) {
            return Optional.of(idmanCookie.getValue());
        }
        return Optional.empty();
    }

    public static class Builder extends AuthFilter.AuthFilterBuilder<String, ServiceUserPrincipal, IdmanAuthFilter> {
        private final IdmanClientConfig authenticationConfig;

        public Builder(IdmanClientConfig authenticationConfig) {
            this.authenticationConfig = authenticationConfig;
        }

        protected IdmanAuthFilter newInstance() {
            return new IdmanAuthFilter(authenticationConfig);
        }
    }
}
