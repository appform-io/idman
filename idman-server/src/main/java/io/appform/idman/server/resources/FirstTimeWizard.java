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

import io.appform.idman.model.AuthMode;
import io.appform.idman.model.UserType;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.db.*;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.vyarus.guicey.gsp.views.template.Template;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Provider;
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
@Path("/ui")
@Template
@Produces(MediaType.TEXT_HTML)
@PermitAll
public class FirstTimeWizard {

    private final Provider<UserInfoStore> userStore;
    private final Provider<PasswordStore> passwordStore;
    private final Provider<UserRoleStore> userRoleStore;
    private final Provider<ServiceStore> serviceStore;
    private final Provider<RoleStore> roleStore;
    private final AuthenticationConfig authenticationConfig;

    @Inject
    public FirstTimeWizard(
            Provider<UserInfoStore> userStore,
            Provider<PasswordStore> passwordStore,
            Provider<UserRoleStore> userRoleStore,
            Provider<ServiceStore> serviceStore,
            Provider<RoleStore> roleStore,
            AuthenticationConfig authenticationConfig) {
        this.userStore = userStore;
        this.passwordStore = passwordStore;
        this.userRoleStore = userRoleStore;
        this.serviceStore = serviceStore;
        this.roleStore = roleStore;
        this.authenticationConfig = authenticationConfig;
    }

    @Path("/setup")
    @GET
    @UnitOfWork
    public Response setupScreen() {
        if (null != serviceStore.get().get("IDMAN").orElse(null)) {
            return Response.seeOther(redirectToHome()).build();
        }
        log.info("Entering first time setup");
        return Response.ok(new TemplateView("templates/createadminuser.hbs")).build();
    }

    @POST
    @Path("/setup")
    @UnitOfWork
    @SneakyThrows
    public Response setup(
            @HeaderParam("Referer") final URI referer,
            @FormParam("email") final String email,
            @FormParam("name") final String name,
            @FormParam("password") final String password) {
        if (null != serviceStore.get().get("IDMAN").orElse(null)) {
            return Response.seeOther(redirectToHome()).build();
        }
        val redirectUri = UriBuilder.fromUri(referer).replacePath("/apis/idman/auth/callback").build();
        val service = serviceStore.get().create("IDMan",
                                                "Identity management service",
                                                redirectUri.toASCIIString())
                .orElse(null);
        Objects.requireNonNull(service);
        val adminRole = roleStore.get()
                .create(service.getServiceId(), "Admin", "Administrator for IDMan")
                .orElse(null);
        Objects.requireNonNull(adminRole);
        val userRole = roleStore.get()
                .create(service.getServiceId(), "User", "General user")
                .orElse(null);
        Objects.requireNonNull(userRole);
        val user = userStore.get()
                .create(UUID.randomUUID().toString(), email, name, UserType.HUMAN, AuthMode.PASSWORD, false)
                .orElse(null);
        Objects.requireNonNull(user);
        passwordStore.get()
                .set(user.getUserId(), password);
        userRoleStore.get()
                .mapUserToRole(user.getUserId(), service.getServiceId(), adminRole.getRoleId(), "SETUP_WIZ");
        log.info("First time setup completed");
        return Response.seeOther(redirectToHome()).build();
    }

    private URI redirectToHome() {
        return URI.create(authenticationConfig.getServer());
    }

}
