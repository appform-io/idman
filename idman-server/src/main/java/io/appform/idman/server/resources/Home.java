package io.appform.idman.server.resources;

import com.google.common.base.Strings;
import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.model.AuthMode;
import io.appform.idman.model.UserType;
import io.appform.idman.server.auth.IdmanRoles;
import io.appform.idman.server.db.*;
import io.appform.idman.server.db.model.StoredRole;
import io.appform.idman.server.db.model.StoredService;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.db.model.StoredUserRole;
import io.appform.idman.server.utils.Utils;
import io.appform.idman.server.views.*;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import ru.vyarus.guicey.gsp.views.template.Template;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 */
@Slf4j
@Path("/ui")
@Template
@Produces(MediaType.TEXT_HTML)
@PermitAll
public class Home {

    private final Provider<ServiceStore> serviceStore;
    private final Provider<RoleStore> roleStore;
    private final Provider<UserInfoStore> userInfoStore;
    private final Provider<PasswordStore> passwordStore;
    private final Provider<UserRoleStore> userRoleStore;

    @Inject
    public Home(
            Provider<ServiceStore> serviceStore,
            Provider<RoleStore> roleStore,
            Provider<UserInfoStore> userInfoStore,
            Provider<PasswordStore> passwordStore,
            Provider<UserRoleStore> userRoleStore) {
        this.serviceStore = serviceStore;
        this.roleStore = roleStore;
        this.userInfoStore = userInfoStore;
        this.passwordStore = passwordStore;
        this.userRoleStore = userRoleStore;
    }

    @GET
    @UnitOfWork
    public Response home(
            @Auth final ServiceUserPrincipal principal,
            @QueryParam("redirect") @Size(max = 4096) final String redirect) {
        val idmanUser = principal.getServiceUser();
        val userId = idmanUser.getUser().getId();
        val currentUser = userInfoStore.get().get(userId).orElse(null);
        if (null == currentUser || currentUser.isDeleted()) {
            return Response.seeOther(URI.create("/auth/login")).build();
        }
        if (currentUser.getAuthState().getAuthState().equals(AuthState.EXPIRED)) {
            return redirectToPasswordChangePage(userId);
        }
        if (!Strings.isNullOrEmpty(redirect)) {
            return Response.seeOther(URI.create(redirect)).build();
        }
        return Response.ok(
                new HomeView(
                        serviceStore.get().list(false),
                        userInfoStore.get().list(false),
                        principal.getServiceUser()))
                .build();
    }

    @Path("/services")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response createService(
            @FormParam("newServiceName") @NotNull @Size(min = 1, max = 45) final String newServiceName,
            @FormParam("newServiceDescription") @NotNull @Size(min = 1, max = 255) final String newServiceDescription,
            @FormParam("newServiceCallbackUrl") @NotNull @Size(min = 1, max = 255) final String newServiceCallbackUrl) {
        val service = serviceStore.get()
                .create(newServiceName, newServiceDescription, newServiceCallbackUrl)
                .orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        return redirectToServicePage(service.getServiceId());
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
        val service = serviceStore.get().get(serviceId).orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        val serviceRoleMappings = userRoleStore.get().getServiceRoleMappings(serviceId);
        val roles = roleStore.get().list(serviceId, false);
        val roleMap = roles.stream()
                .collect(Collectors.toMap(StoredRole::getRoleId, Function.identity()));
        val allUsers = userInfoStore.get().list(false);
        val mappedUserDetails = allUsers
                .stream()
                .collect(Collectors.toMap(StoredUser::getUserId, Function.identity()));

        return Response.ok(
                new ServiceDetailsView(
                        service,
                        roles,
                        allUsers,
                        serviceRoleMappings.stream()
                                .map(mapping -> {
                                    val user = mappedUserDetails.get(mapping.getUserId());
                                    val role = roleMap.get(mapping.getRoleId());
                                    if (null == user || null == role) {
                                        return null;
                                    }
                                    return new ServiceDetailsView.ServiceUser(user, role);
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()),
                        principal.getServiceUser()))
                .build();
    }

    @Path("/services/{serviceId}/update/description")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response updateServiceDescription(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @FormParam("newServiceDescription") @NotNull @Size(min = 1, max = 255) final String newServiceDescription) {
        val service = serviceStore.get().updateDescription(serviceId, newServiceDescription)
                .orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        return redirectToServicePage(service.getServiceId());
    }

    @Path("/services/{serviceId}/update/callback")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response updateServiceCallbackUrl(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @FormParam("newServiceCallbackUrl") @NotNull @Size(min = 1, max = 255) final String newServiceCallbackUrl) {
        val service = serviceStore.get().updateCallbackUrl(serviceId, newServiceCallbackUrl)
                .orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        return redirectToServicePage(service.getServiceId());
    }

    @Path("/services/{serviceId}/update/secret")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response updateServiceSecret(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId) {
        val service = serviceStore.get().updateSecret(serviceId)
                .orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        return redirectToServicePage(service.getServiceId());
    }

    @Path("/services/{serviceId}/delete")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response deleteService(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId) {
        val status = serviceStore.get().delete(serviceId);
        log.info("Service {} deletion status: {}", serviceId, status);
        return redirectToHome();
    }

    @Path("/services/{serviceId}/roles")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response createRole(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @FormParam("newRoleName") @NotEmpty @Size(max = 45) final String newRoleName,
            @FormParam("newRoleDescription") @NotEmpty @Size(max = 45) final String newRoleDescription) {
        val service = serviceStore.get().get(serviceId)
                .orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        val role = roleStore.get().create(serviceId, newRoleName, newRoleDescription)
                .orElse(null);
        if (role != null) {
            log.debug("Role {} created for service: {}", role.getId(), serviceId);
        }
        else {
            log.error("Error creating role {} for service {}", newRoleName, serviceId);
        }
        return redirectToServicePage(serviceId);
    }

    @Path("/services/{serviceId}/roles/{roleId}/update")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response updateRole(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @PathParam("roleId") @NotEmpty @Size(max = 45) final String roleId,
            @FormParam("roleDescription") @NotEmpty @Size(max = 45) final String roleDescription) {
        val service = serviceStore.get().get(serviceId)
                .orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        val role = roleStore.get().update(serviceId, roleId, roleDescription)
                .orElse(null);
        if (role != null) {
            log.debug("Role {} updated for service: {}", roleId, serviceId);
        }
        else {
            log.error("Error updating role {} for service {}", roleId, serviceId);
        }
        return redirectToServicePage(serviceId);
    }

    @Path("/services/{serviceId}/roles/{roleId}/delete")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response deleteRole(
            @PathParam("serviceId") @NotEmpty @Size(max = 45) final String serviceId,
            @PathParam("roleId") @NotEmpty @Size(max = 45) final String roleId) {
        val service = serviceStore.get().get(serviceId)
                .orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        val status = roleStore.get().delete(serviceId, roleId);
        if (status) {
            log.debug("Role {} deleted for service: {}", roleId, serviceId);
        }
        else {
            log.error("Error deleting role {} for service {}", roleId, serviceId);
        }
        return redirectToServicePage(serviceId);
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
        val userId = Utils.hashedId(email);
        val user = userInfoStore.get()
                .create(userId, email, name, UserType.HUMAN, AuthMode.PASSWORD)
                .orElse(null);
        if (null == user) {
            return redirectToHome();
        }
        passwordStore.get().set(userId, password);
        return redirectToUserPage(userId);
    }

    @Path("/users/{userId}")
    @GET
    @UnitOfWork
    public Response userDetails(
            @Auth final ServiceUserPrincipal principal,
            @PathParam("userId") @NotEmpty @Size(max = 255) final String userId) {
        val user = userInfoStore.get().get(userId).orElse(null);
        if (null == user) {
            return redirectToHome();
        }
        val mappings = userRoleStore.get().getUserRoles(userId);
        val services = serviceStore.get()
                .get(mappings.stream()
                             .map(StoredUserRole::getServiceId)
                             .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(StoredService::getServiceId, Function.identity()));
        val roles = roleStore.get()
                .get(mappings.stream()
                             .map(StoredUserRole::getRoleId)
                             .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(StoredRole::getRoleId, Function.identity()));
        return Response.ok(
                new UserDetailsView(
                        user,
                        mappings.stream()
                                .map(mapping -> {
                                    val service = services.get(mapping.getServiceId());
                                    val role = roles.get(mapping.getRoleId());
                                    if (service == null || role == null) {
                                        return null;
                                    }
                                    return new UserDetailsView.UserServices(service, role);
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()),
                        principal.getServiceUser()))
                .build();
    }

    @Path("/users/{userId}/update")
    @POST
    @UnitOfWork
    public Response updateUser(
            @Auth final ServiceUserPrincipal sessionUser,
            @PathParam("userId") @NotEmpty @Size(max = 255) final String userId,
            @FormParam("name") @NotEmpty @Size(max = 255) final String name) {
        val userstore = userInfoStore.get();
        var user = userstore.get(userId).orElse(null);
        if (null == user) {
            return redirectToHome();
        }
        val sessionUserId = sessionUser.getServiceUser().getUser().getId();
        if (!sessionUserId.equals(userId)
                && !sessionUser.getServiceUser().getRole().equals(IdmanRoles.ADMIN)) {
            log.warn("Non admin user {} tried to change name for {}", sessionUserId, userId);
            return redirectToUserPage(userId);
        }
        user = userstore.updateName(userId, name).orElse(null);
        if (null == user) {
            log.warn("Name not updated for: {}", userId);
            return redirectToHome();
        }
        return redirectToUserPage(userId);
    }

    @Path("/users/{userId}/delete")
    @POST
    @UnitOfWork
    @RolesAllowed(IdmanRoles.ADMIN)
    public Response deleteUser(
            @PathParam("userId") @NotEmpty @Size(max = 255) final String userId) {
        val userstore = userInfoStore.get();
        var user = userstore.get(userId).orElse(null);
        if (null == user) {
            return redirectToHome();
        }
        val status = userstore.deleteUser(userId);
        log.info("Deletion status for: {} is: {}", userId, status);
        return redirectToHome();
    }

    @Path("/users/{userId}/update/password")
    @GET
    @UnitOfWork
    public Response passwordChangePage(
            @Auth final ServiceUserPrincipal principal,
            @PathParam("userId") @NotEmpty @Size(max = 45) final String userId) {
        val user = userInfoStore.get().get(userId).orElse(null);
        val idmanUser = principal.getServiceUser();
        if ((null == user
                || !idmanUser.getUser().getId().equals(userId))
                && !idmanUser.getRole().equals(IdmanRoles.ADMIN)) {
            return redirectToUserPage(userId);
        }
        val skipOld = !idmanUser.getUser().getId().equals(userId);
        return Response.ok(new PasswordChangeView(user, skipOld, principal.getServiceUser())).build();
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
        val userstore = userInfoStore.get();
        var user = userstore.get(userId).orElse(null);
        if (null == user || !sessionUser.getServiceUser().getUser().getId().equals(userId)) {
            return redirectToHome();
        }
        if (!newPassword.equals(newPasswordConf)) {
            log.warn("New passwords do not match for: {}", userId);
        }
        val status = passwordStore.get()
                .update(userId, oldPassword, newPassword);
        log.info("Password change state for user {} is {}", userId, status);
        if (!status) {
            return redirectToPasswordChangePage(userId);
        }
        userstore.updateAuthState(userId, userAuthState -> {
            userAuthState.setFailedAuthCount(0);
            userAuthState.setAuthState(AuthState.ACTIVE);
        });
        return redirectToUserPage(userId);
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
        val userStore = userInfoStore.get();
        var user = userStore.get(userId).orElse(null);
        if (null == user
                || sessionUser.getServiceUser().getUser().getId().equals(userId)) {
            return redirectToUserPage(userId);
        }
        if (!newPassword.equals(newPasswordConf)) {
            log.warn("New passwords do not match for: {}", userId);
        }
        passwordStore.get().set(userId, newPassword);
        userStore.updateAuthState(userId, userAuthState -> {
            userAuthState.setAuthState(AuthState.EXPIRED);
            userAuthState.setFailedAuthCount(0);
        });
        return redirectToUserPage(userId);
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
        val service = serviceStore.get().get(serviceId).orElse(null);
        val role = roleStore.get().get(serviceId, roleId).orElse(null);
        val user = userInfoStore.get().get(userId).orElse(null);
        if (service == null || service.isDeleted()
                || role == null || role.isDeleted()
                || user == null || user.isDeleted()) {
            return redirectToHome();
        }
        userRoleStore.get()
                .mapUserToRole(userId, serviceId, roleId, sessionUser.getServiceUser().getUser().getId());
        log.info("Mapping user {} to role: {}/{} completed", userId, serviceId, roleId);
        if (null == referer || referer.toString().isEmpty()) {
            return redirectToUserPage(userId);
        }
        return Response.seeOther(URI.create(referer.getPath())).build();
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
        val service = serviceStore.get().get(serviceId).orElse(null);
        val role = roleStore.get().get(serviceId, roleId).orElse(null);
        val user = userInfoStore.get().get(userId).orElse(null);
        if (service == null || service.isDeleted()
                || role == null || role.isDeleted()
                || user == null || user.isDeleted()) {
            return redirectToHome();
        }
        val status = userRoleStore.get().unmapUserFromRole(userId, serviceId);
        log.info("Status for unmapping user {} from role: {}/{}: {}", userId, serviceId, roleId, status);
        if (null == referer || referer.toString().isEmpty()) {
            return redirectToUserPage(userId);
        }
        return Response.seeOther(URI.create(referer.getPath())).build();
    }

    public static Response redirectToHome() {
        return Response.seeOther(URI.create("/")).build();
    }

    public static Response redirectToServicePage(String serviceId) {
        return Response.seeOther(URI.create("/services/" + serviceId)).build();
    }

    public static Response redirectToUserPage(String userId) {
        return Response.seeOther(URI.create("/users/" + userId)).build();
    }

    private Response redirectToPasswordChangePage(String userId) {
        return Response.seeOther(URI.create("/users/" + userId + "/update/password")).build();
    }

}
