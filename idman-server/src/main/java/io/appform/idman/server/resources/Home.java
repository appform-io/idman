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

import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.server.engine.Engine;
import io.appform.idman.server.auth.IdmanRoles;
import io.appform.idman.server.engine.ViewEngineResponseTranslator;
import io.appform.idman.server.views.NewUserView;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import ru.vyarus.guicey.gsp.views.template.Template;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 *
 */
@Slf4j
@Path("/ui")
@Template
@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
@PermitAll
public class Home {

    private final Engine engine;
    private final ViewEngineResponseTranslator translator;

    @Inject
    public Home(Engine engine, ViewEngineResponseTranslator translator) {
        this.engine = engine;
        this.translator = translator;
    }

    @GET
    @UnitOfWork
    public Response home(
            @Auth final ServiceUserPrincipal principal,
            @QueryParam("redirect") @Size(max = 4096) final String redirect) {
        return translator.translate(engine.renderHome(principal, redirect));
    }

    @Path("/services")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response createService(
            @FormParam("newServiceName") @NotNull @Size(min = 1, max = 45) final String newServiceName,
            @FormParam("newServiceDescription") @NotNull @Size(min = 1, max = 255) final String newServiceDescription,
            @FormParam("newServiceCallbackUrl") @NotNull @Size(min = 1, max = 255) final String newServiceCallbackUrl) {
        return translator.translate(engine.createService(newServiceName, newServiceDescription, newServiceCallbackUrl));
    }

    @Path("/services/new")
    @GET
    public Response newService() {
        return Response.ok(new TemplateView("templates/newservice.hbs")).build();
    }

    @Path("/services/{serviceId}")
    @GET
    @UnitOfWork
    public Response serviceDetails(
            @Auth final ServiceUserPrincipal principal,
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId) {
        return translator.translate(engine.renderServiceDetails(principal, serviceId));
    }

    @Path("/services/{serviceId}/update/description")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response updateServiceDescription(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @FormParam("newServiceDescription") @NotNull @Size(min = 1, max = 255) final String newServiceDescription) {
        return translator.translate(engine.updateServiceDescription(serviceId, newServiceDescription));
    }

    @Path("/services/{serviceId}/update/callback")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response updateServiceCallbackUrl(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @FormParam("newServiceCallbackUrl") @NotNull @Size(min = 1, max = 255) final String newServiceCallbackUrl) {
        return translator.translate(engine.updateServiceCallbackUrl(serviceId, newServiceCallbackUrl));
    }


    @Path("/services/{serviceId}/update/secret")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response updateServiceSecret(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId) {
        return translator.translate(engine.regenerateServiceSecret(serviceId));
    }

    @Path("/services/{serviceId}/delete")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response deleteService(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId) {
        return translator.translate(engine.deleteService(serviceId));
    }

    @Path("/services/{serviceId}/roles")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response createRole(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @FormParam("newRoleName") @NotEmpty @Size(max = 45) final String newRoleName,
            @FormParam("newRoleDescription") @NotEmpty @Size(max = 45) final String newRoleDescription) {
        return translator.translate(engine.createRole(serviceId, newRoleName, newRoleDescription));
    }

    @Path("/services/{serviceId}/roles/{roleId}/update")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response updateRole(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @PathParam("roleId") @NotEmpty @Size(max = 45) final String roleId,
            @FormParam("roleDescription") @NotEmpty @Size(max = 45) final String roleDescription) {

        return translator.translate(engine.updateRole(serviceId, roleId, roleDescription));
    }

    @Path("/services/{serviceId}/roles/{roleId}/delete")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response deleteRole(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @PathParam("roleId") @NotEmpty @Size(max = 45) final String roleId) {
        return translator.translate(engine.deleteRole(serviceId, roleId));
    }

    @Path("/users/new")
    @GET
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response newUser() {
        return Response.ok(new NewUserView("/users/human")).build();
    }

    @Path("/users/human")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response createHumanUser(
            @FormParam("email") @Email @NotEmpty @Size(max = 255) final String email,
            @FormParam("name") @NotEmpty @Size(max = 255) final String name,
            @FormParam("password") @NotEmpty final String password) {
        return translator.translate(engine.createHumanUser(email, name, password));
    }

    @Path("/users/{userId}")
    @GET
    @UnitOfWork
    public Response userDetails(
            @Auth final ServiceUserPrincipal principal,
            @PathParam("userId") @NotEmpty @Size(max = 255) final String userId) {
        return translator.translate(engine.userDetails(principal, userId));
    }

    @Path("/users/{userId}/update")
    @POST
    @UnitOfWork
    public Response updateUser(
            @Auth final ServiceUserPrincipal sessionUser,
            @PathParam("userId") @NotEmpty @Size(max = 255) final String userId,
            @FormParam("name") @NotEmpty @Size(max = 255) final String name) {
        return translator.translate(engine.updateUser(sessionUser, userId, name));
    }

    @Path("/users/{userId}/delete")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response deleteUser(
            @PathParam("userId") @NotEmpty @Size(max = 255) final String userId) {
        return translator.translate(engine.deleteUser(userId));
    }

    @Path("/users/{userId}/update/password")
    @GET
    @UnitOfWork
    public Response renderPasswordChangePage(
            @Auth final ServiceUserPrincipal principal,
            @PathParam("userId") @NotEmpty @Size(max = 45) final String userId) {
        return translator.translate(engine.renderPasswordChangePage(principal, userId));
    }

    @Path("/users/{userId}/update/password")
    @POST
    @UnitOfWork
    public Response changePassword(
            @Auth final ServiceUserPrincipal sessionUser,
            @PathParam("userId") @NotEmpty @Size(max = 255) final String userId,
            @FormParam("oldPassword") @NotEmpty @Size(max = 255) final String oldPassword,
            @FormParam("newPassword") @NotEmpty @Size(max = 255) final String newPassword,
            @FormParam("newPasswordConf") @NotEmpty @Size(max = 255) final String newPasswordConf) {
        return translator.translate(engine.changePassword(sessionUser,
                                                          userId,
                                                          oldPassword,
                                                          newPassword,
                                                          newPasswordConf));
    }

    @Path("/users/{userId}/update/password/forced")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response changePasswordForced(
            @Auth final ServiceUserPrincipal sessionUser,
            @PathParam("userId") @NotEmpty @Size(max = 255) final String userId,
            @FormParam("newPassword") @NotEmpty @Size(max = 255) final String newPassword,
            @FormParam("newPasswordConf") @NotEmpty @Size(max = 255) final String newPasswordConf) {
        return translator.translate(engine.changePasswordForced(sessionUser, userId, newPassword, newPasswordConf));
    }

    @Path("/roles/{serviceId}/map")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response mapUserToRole(
            @Auth final ServiceUserPrincipal sessionUser,
            @HeaderParam("Referer") final URI referer,
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @FormParam("roleId") @NotEmpty @Size(max = 45) final String roleId,
            @FormParam("userId") @NotEmpty @Size(max = 45) final String userId) {
        return translator.translate(engine.mapUserToRole(sessionUser, referer, serviceId, roleId, userId));
    }

    @Path("/roles/{serviceId}/{roleId}/unmap/{userId}")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response unmapUserFromRole(
            @HeaderParam("Referer") final URI referer,
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @PathParam("roleId") @NotEmpty @Size(max = 45) final String roleId,
            @PathParam("userId") @NotEmpty @Size(max = 45) final String userId) {
        return translator.translate(engine.unmapUserFromRole(referer, serviceId, roleId, userId));
    }

}
