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

package io.appform.idman.server.resources;

import com.google.common.base.Strings;
import io.appform.idman.model.AuthMode;
import io.appform.idman.server.auth.AuthenticationProviderRegistry;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.impl.PasswordAuthInfo;
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.utils.Utils;
import io.appform.idman.server.views.LoginScreenView;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
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

    private final AuthenticationConfig authenticationConfig;
    private final Provider<AuthenticationProviderRegistry> authenticators;
    private final Provider<ServiceStore> serviceStore;

    @Inject
    public Auth(
            AuthenticationConfig authenticationConfig,
            Provider<AuthenticationProviderRegistry> authenticators,
            Provider<ServiceStore> serviceStore) {
        this.authenticationConfig = authenticationConfig;
        this.authenticators = authenticators;
        this.serviceStore = serviceStore;
    }

    @Path("/login/{serviceId}")
    @GET
    public Response loginScreen(
            @PathParam("serviceId") @NotEmpty @Size(max = 255) final String serviceId,
            @QueryParam("redirect") @NotEmpty @Size(max = 4096) final String redirect,
            @QueryParam("clientSessionId") @NotEmpty @Size(max = 255) final String clientSessionId,
            @QueryParam("error") @Size(max = 255) final String error) {
        return Response.ok(new LoginScreenView(error, serviceId, clientSessionId, redirect)).build();
    }

    @Path("/login/password")
    @POST
    @UnitOfWork
    public Response passwordLogin(
            @HeaderParam("Referer") final URI referer,
            @FormParam("email") @NotEmpty @Size(max = 255) final String email,
            @FormParam("password") @NotEmpty @Size(max = 255) final String password,
            @FormParam("redirect") @NotEmpty @Size(max = 4096) final String redirect,
            @FormParam("serviceId") @Size(max = 255) final String serviceId,
            @FormParam("clientSessionId") @Size(max = 255) final String clientSessionId) {
        val service = Strings.isNullOrEmpty(serviceId)
                      ? null
                      : serviceStore.get().get(serviceId).orElse(null);
        if(null == service || service.isDeleted()) {
            return Response.seeOther(errorUri(referer, "Invalid service")).build();
        }
        val authenticationProvider = authenticators.get()
                .provider(AuthMode.PASSWORD)
                .orElse(null);
        Objects.requireNonNull(authenticationProvider, "No authenticator found");
        val session = authenticationProvider.login(
                new PasswordAuthInfo(email, password, serviceId, clientSessionId),
                UUID.randomUUID().toString())
                .orElse(null);
        if (session == null) {
            return Response.seeOther(errorUri(referer, "Invalid credentials")).build();
        }
        val token = Utils.createJWT(session, authenticationConfig.getJwt());
        val uri = UriBuilder.fromUri(service.getCallbackUrl())
                .queryParam("clientSessionId", session.getClientSessionId())
                .queryParam("code", token)
                .build();
        return Response.seeOther(uri).build();
    }

    private URI errorUri(URI referrer, String error) {
        val builder = UriBuilder.fromUri(referrer);
        builder.replaceQueryParam("error", error);
        return builder.build();
    }
}
