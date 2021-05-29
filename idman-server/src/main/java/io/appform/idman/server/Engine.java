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

package io.appform.idman.server;

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
import io.appform.idman.server.views.HomeView;
import io.appform.idman.server.views.PasswordChangeView;
import io.appform.idman.server.views.ServiceDetailsView;
import io.appform.idman.server.views.UserDetailsView;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 */
@Singleton
@Slf4j
public class Engine {
    private final Provider<ServiceStore> serviceStore;
    private final Provider<RoleStore> roleStore;
    private final Provider<UserInfoStore> userInfoStore;
    private final Provider<PasswordStore> passwordStore;
    private final Provider<UserRoleStore> userRoleStore;

    @Inject
    public Engine(
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

    public Response renderHome(final ServiceUserPrincipal principal, final String redirect) {
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

    public Response createService(String newServiceName, String newServiceDescription, String newServiceCallbackUrl) {
        val service = serviceStore.get()
                .create(newServiceName, newServiceDescription, newServiceCallbackUrl)
                .orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        return redirectToServicePage(service.getServiceId());
    }

    public Response renderServiceDetails(final ServiceUserPrincipal principal, final String serviceId) {
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

    public Response updateServiceDescription(final String serviceId, final String newServiceDescription) {
        val service = serviceStore.get().updateDescription(serviceId, newServiceDescription)
                .orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        return redirectToServicePage(service.getServiceId());
    }

    public Response updateServiceCallbackUrl(String serviceId, String newServiceCallbackUrl) {
        val service = serviceStore.get().updateCallbackUrl(serviceId, newServiceCallbackUrl)
                .orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        return redirectToServicePage(service.getServiceId());
    }

    public Response regenerateServiceSecret(String serviceId) {
        val service = serviceStore.get().updateSecret(serviceId)
                .orElse(null);
        if (null == service) {
            return redirectToHome();
        }
        return redirectToServicePage(service.getServiceId());
    }

    public Response deleteService(@PathParam("serviceId") @NotEmpty @Size(max = 45) String serviceId) {
        val status = serviceStore.get().delete(serviceId);
        log.info("Service {} deletion status: {}", serviceId, status);
        return redirectToHome();
    }

    public Response createRole(String serviceId, String newRoleName, String newRoleDescription) {
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

    public Response updateRole(final String serviceId, final String roleId, final String roleDescription) {
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

    public Response deleteRole(final String serviceId, final String roleId) {
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

    public Response createHumanUser(final String email, final String name, final String password) {
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

    public Response userDetails(final ServiceUserPrincipal principal, final String userId) {
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

    public Response updateUser(final ServiceUserPrincipal sessionUser, final String userId, final String name) {
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

    public Response deleteUser(final String userId) {
        val userstore = userInfoStore.get();
        var user = userstore.get(userId).orElse(null);
        if (null == user) {
            return redirectToHome();
        }
        val status = userstore.deleteUser(userId);
        log.info("Deletion status for: {} is: {}", userId, status);
        return redirectToHome();
    }

    public Response renderPasswordChangePage(final ServiceUserPrincipal principal, final String userId) {
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


    public Response changePassword(
            final ServiceUserPrincipal sessionUser,
            final String userId,
            final String oldPassword,
            final String newPassword,
            final String newPasswordConf) {
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

    public Response changePasswordForced(
            final ServiceUserPrincipal sessionUser,
            final String userId,
            final String newPassword,
            final String newPasswordConf) {
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

    public Response mapUserToRole(
            final ServiceUserPrincipal sessionUser,
            final URI referer,
            final String serviceId,
            final String roleId,
            final String userId) {
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

    public Response unmapUserFromRole(
            final URI referer, final String serviceId, final String roleId, final String userId) {
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

    private static Response redirectToHome() {
        return Response.seeOther(URI.create("/")).build();
    }

    private static Response redirectToServicePage(final String serviceId) {
        return Response.seeOther(URI.create("/services/" + serviceId)).build();
    }

    private static Response redirectToUserPage(final String userId) {
        return Response.seeOther(URI.create("/users/" + userId)).build();
    }

    private static Response redirectToPasswordChangePage(final String userId) {
        return Response.seeOther(URI.create("/users/" + userId + "/update/password")).build();
    }
}
