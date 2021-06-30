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

package io.appform.idman.server.engine;

import com.google.common.base.Strings;
import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.model.AuthMode;
import io.appform.idman.model.TokenType;
import io.appform.idman.model.UserType;
import io.appform.idman.server.auth.IdmanRoles;
import io.appform.idman.server.auth.TokenManager;
import io.appform.idman.server.db.*;
import io.appform.idman.server.db.model.StoredRole;
import io.appform.idman.server.db.model.StoredService;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.db.model.StoredUserRole;
import io.appform.idman.server.engine.results.*;
import io.appform.idman.server.utils.Utils;
import io.appform.idman.server.views.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import javax.ws.rs.PathParam;
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
    private final Provider<SessionStore> sessionStore;
    private final Provider<TokenManager> tokenManager;

    @Inject
    public Engine(
            Provider<ServiceStore> serviceStore,
            Provider<RoleStore> roleStore,
            Provider<UserInfoStore> userInfoStore,
            Provider<PasswordStore> passwordStore,
            Provider<UserRoleStore> userRoleStore,
            Provider<SessionStore> sessionStore,
            Provider<TokenManager> tokenManager) {
        this.serviceStore = serviceStore;
        this.roleStore = roleStore;
        this.userInfoStore = userInfoStore;
        this.passwordStore = passwordStore;
        this.userRoleStore = userRoleStore;
        this.sessionStore = sessionStore;
        this.tokenManager = tokenManager;
    }

    public EngineEvalResult renderHome(final ServiceUserPrincipal principal, final String redirect) {
        val idmanUser = principal.getServiceUser();
        val userId = idmanUser.getUser().getId();
        val currentUser = userInfoStore.get().get(userId).orElse(null);
        if (null == currentUser || currentUser.isDeleted()) {
            return new InvalidUser();
        }
        if (currentUser.getAuthState().getAuthState().equals(AuthState.EXPIRED)) {
            return new CredentialsExpired(userId);
        }
        if (!Strings.isNullOrEmpty(redirect)) {
            return new RedirectToParam(redirect);
        }
        return new ViewOpSuccess(
                new HomeView(
                        serviceStore.get().list(false),
                        userInfoStore.get().list(false),
                        principal.getServiceUser()));
    }

    public EngineEvalResult createService(
            String newServiceName,
            String newServiceDescription,
            String newServiceCallbackUrl) {
        val service = serviceStore.get()
                .create(newServiceName, newServiceDescription, newServiceCallbackUrl)
                .orElse(null);
        if (null == service) {
            return new InvalidService();
        }
        return new ServiceOpSuccess(service.getServiceId());
    }

    public EngineEvalResult renderServiceDetails(final ServiceUserPrincipal principal, final String serviceId) {
        val service = serviceStore.get().get(serviceId).orElse(null);
        if (null == service) {
            return new InvalidService();
        }
        val serviceRoleMappings = userRoleStore.get().getServiceRoleMappings(serviceId);
        val roles = roleStore.get().list(serviceId, false);
        val roleMap = roles.stream()
                .collect(Collectors.toMap(StoredRole::getRoleId, Function.identity()));
        val allUsers = userInfoStore.get().list(false);
        val mappedUserDetails = allUsers
                .stream()
                .collect(Collectors.toMap(StoredUser::getUserId, Function.identity()));

        return new ViewOpSuccess(
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
                        principal.getServiceUser()));
    }

    public EngineEvalResult updateServiceDescription(final String serviceId, final String newServiceDescription) {
        val service = serviceStore.get().updateDescription(serviceId, newServiceDescription)
                .orElse(null);
        if (null == service) {
            return new InvalidService();
        }
        return new ServiceOpSuccess(service.getServiceId());
    }

    public EngineEvalResult updateServiceCallbackUrl(String serviceId, String newServiceCallbackUrl) {
        val service = serviceStore.get().updateCallbackUrl(serviceId, newServiceCallbackUrl)
                .orElse(null);
        if (null == service) {
            return new InvalidService();
        }
        return new ServiceOpSuccess(service.getServiceId());
    }

    public EngineEvalResult regenerateServiceSecret(String serviceId) {
        val service = serviceStore.get().updateSecret(serviceId)
                .orElse(null);
        if (null == service) {
            return new InvalidService();
        }
        return new ServiceOpSuccess(service.getServiceId());
    }

    public EngineEvalResult deleteService(@PathParam("serviceId") @NotEmpty @Size(max = 45) String serviceId) {
        val status = serviceStore.get().delete(serviceId);
        if (!status) {
            log.warn("Unable to delete service: {}", serviceId);
            return new GeneralOpFailure();
        }
        return new GeneralOpSuccess();
    }

    public EngineEvalResult createRole(String serviceId, String newRoleName, String newRoleDescription) {
        val service = serviceStore.get().get(serviceId)
                .orElse(null);
        if (null == service) {
            return new InvalidService();
        }
        val role = roleStore.get().create(serviceId, newRoleName, newRoleDescription)
                .orElse(null);
        if (role == null) {
            log.error("Error creating role {} for service {}", newRoleName, serviceId);
            return new RoleOpFailure(serviceId, null);
        }
        log.debug("Role {} created for service: {}", role.getRoleId(), serviceId);
        return new RoleOpSuccess(serviceId, role.getRoleId());
    }

    public EngineEvalResult updateRole(final String serviceId, final String roleId, final String roleDescription) {
        val service = serviceStore.get().get(serviceId)
                .orElse(null);
        if (null == service) {
            return new InvalidService();
        }
        val role = roleStore.get().update(serviceId, roleId, roleDescription)
                .orElse(null);
        if (role == null) {
            log.error("Error updating role {} for service {}", roleId, serviceId);
            return new RoleOpFailure(serviceId, roleId);
        }
        return new RoleOpSuccess(serviceId, roleId);
    }

    public EngineEvalResult deleteRole(final String serviceId, final String roleId) {
        val service = serviceStore.get().get(serviceId)
                .orElse(null);
        if (null == service) {
            return new InvalidService();
        }
        val status = roleStore.get().delete(serviceId, roleId);
        if (!status) {
            log.error("Error deleting role {} for service {}", roleId, serviceId);
            return new RoleOpFailure(serviceId, roleId);
        }
        return new RoleOpSuccess(serviceId, roleId);
    }

    public EngineEvalResult createHumanUser(final String email, final String name, final String password) {
        val userId = Utils.hashedId(email);
        val user = userInfoStore.get()
                .create(userId, email, name, UserType.HUMAN, AuthMode.PASSWORD)
                .orElse(null);
        if (null == user) {
            return new GeneralOpFailure();
        }
        passwordStore.get().set(userId, password);
        userRoleStore.get()
                .mapUserToRole(userId, "IDMAN", IdmanRoles.USER, "IDMAN");
        return new UserOpSuccess(userId);
    }

    public EngineEvalResult createSystemUser(
            final String maintainerEmail,
            final String systemName) {
        val userId = Utils.hashedId(systemName);
        return userInfoStore.get()
                .create(userId, maintainerEmail, systemName, UserType.SYSTEM, AuthMode.TOKEN, false)
                .map(user -> (EngineEvalResult) new UserOpSuccess(userId))
                .orElse(new GeneralOpFailure());
    }

    public EngineEvalResult userDetails(final ServiceUserPrincipal principal, final String userId) {
        val user = userInfoStore.get().get(userId).orElse(null);
        if (null == user) {
            return new GeneralOpFailure();
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
        return new ViewOpSuccess(
                new UserDetailsView(
                        user,
                        mappings.stream()
                                .map(mapping -> {
                                    val service = services.get(mapping.getServiceId());
                                    val role = roles.get(mapping.getRoleId());
                                    if (service == null || role == null) {
                                        return null;
                                    }
                                    return new UserDetailsView.UserService(service, role);
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()),
                        principal.getServiceUser(),
                        sessionStore.get()
                                .sessionsForUser(userId,
                                                 user.getUserType() == UserType.HUMAN
                                                 ? TokenType.DYNAMIC
                                                 : TokenType.STATIC)
                                .stream()
                                .map(session -> {
                                    val service = services.get(session.getServiceId());
                                    if (null == service) {
                                        return null;
                                    }
                                    return new UserDetailsView.UserSession(service, session);
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())));
    }

    public EngineEvalResult updateUser(final ServiceUserPrincipal sessionUser, final String userId, final String name) {
        val userStore = userInfoStore.get();
        var user = userStore.get(userId).orElse(null);
        if (null == user) {
            return new GeneralOpFailure();
        }
        val sessionUserId = sessionUser.getServiceUser().getUser().getId();
        if (!sessionUserId.equals(userId)
                && !sessionUser.getServiceUser().getRole().equals(IdmanRoles.ADMIN)) {
            log.warn("Non admin user {} tried to change name for {}", sessionUserId, userId);
            return new UserOpFailure(userId);
        }
        user = userStore.updateName(userId, name).orElse(null);
        if (null == user) {
            log.warn("Name not updated for: {}", userId);
            return new UserOpFailure(userId);
        }
        return new UserOpSuccess(userId);
    }

    public EngineEvalResult deleteUser(final String userId) {
        val userStore = userInfoStore.get();
        var user = userStore.get(userId).orElse(null);
        if (null == user) {
            return new GeneralOpFailure();
        }
        val status = userStore.deleteUser(userId);
        log.info("Deletion status for: {} is: {}", userId, status);
        return new GeneralOpSuccess();
    }

    public EngineEvalResult renderPasswordChangePage(final ServiceUserPrincipal principal, final String userId) {
        val user = userInfoStore.get().get(userId).orElse(null);
        val idmanUser = principal.getServiceUser();
        if (null == user
                || (!idmanUser.getUser().getId().equals(userId))
                && !idmanUser.getRole().equals(IdmanRoles.ADMIN)) {
            return new UserOpFailure(userId);
        }
        val skipOld = !idmanUser.getUser().getId().equals(userId);
        return new ViewOpSuccess(new PasswordChangeView(user, skipOld, principal.getServiceUser()));
    }


    public EngineEvalResult changePassword(
            final ServiceUserPrincipal sessionUser,
            final String userId,
            final String oldPassword,
            final String newPassword,
            final String newPasswordConf) {
        val userStore = userInfoStore.get();
        var user = userStore.get(userId).orElse(null);
        if (null == user || !sessionUser.getServiceUser().getUser().getId().equals(userId)) {
            return new CredentialsExpired(userId);
        }
        if (!newPassword.equals(newPasswordConf) || newPassword.equals(oldPassword)) {
            log.warn("New passwords do not match for: {}", userId);
            return new CredentialsExpired(userId);
        }
        val status = passwordStore.get()
                .update(userId, oldPassword, newPassword);
        if (!status) {
            log.info("Password change failed for user {} is {}", userId, status);
            return new CredentialsExpired(userId);
        }
        userStore.updateAuthState(userId, userAuthState -> {
            userAuthState.setFailedAuthCount(0);
            userAuthState.setAuthState(AuthState.ACTIVE);
        });
        return new UserOpSuccess(userId);
    }

    public EngineEvalResult changePasswordForced(
            final ServiceUserPrincipal principal,
            final String userId,
            final String newPassword,
            final String newPasswordConf) {
        val userStore = userInfoStore.get();
        var user = userStore.get(userId).orElse(null);
        val sessionUser = principal.getServiceUser();
        if (null == user
                || sessionUser.getUser().getId().equals(userId)
                || !sessionUser.getRole().equals(IdmanRoles.ADMIN)) {
            return new UserOpFailure(userId);
        }
        if (!newPassword.equals(newPasswordConf)) {
            log.warn("New passwords do not match for: {}", userId);
            return new UserOpFailure(userId);
        }
        passwordStore.get().set(userId, newPassword);
        userStore.updateAuthState(userId, userAuthState -> {
            userAuthState.setAuthState(AuthState.EXPIRED);
            userAuthState.setFailedAuthCount(0);
        });
        return new UserOpSuccess(userId);
    }

    public EngineEvalResult mapUserToRole(
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
            return new GeneralOpFailure();
        }
        userRoleStore.get()
                .mapUserToRole(userId, serviceId, roleId, sessionUser.getServiceUser().getUser().getId());
        log.info("Mapping user {} to role: {}/{} completed", userId, serviceId, roleId);
        if (null == referer || referer.toString().isEmpty()) {
            return new UserOpSuccess(userId);
        }
        return new RedirectToParam(referer.getPath());
    }

    public EngineEvalResult unmapUserFromRole(
            final URI referer, final String serviceId, final String roleId, final String userId) {
        val service = serviceStore.get().get(serviceId).orElse(null);
        val role = roleStore.get().get(serviceId, roleId).orElse(null);
        val user = userInfoStore.get().get(userId).orElse(null);
        if (service == null || service.isDeleted()
                || role == null || role.isDeleted()
                || user == null || user.isDeleted()) {
            return new GeneralOpFailure();
        }
        val status = userRoleStore.get().unmapUserFromRole(userId, serviceId);
        log.info("Status for unmapping user {} from role: {}/{}: {}", userId, serviceId, roleId, status);
        if (null == referer || referer.toString().isEmpty()) {
            return new UserOpSuccess(userId);
        }
        return new RedirectToParam(referer.getPath());
    }

    public EngineEvalResult createStaticSession(final String userId, final String serviceId) {
        return tokenManager.get()
                .createToken(serviceId, userId, null, TokenType.STATIC, null)
                .map(session -> (EngineEvalResult) new TokenOpSuccess(session.getSessionId(), serviceId, userId))
                .orElse(new UserOpFailure(userId));
    }

    public EngineEvalResult viewToken(final String serviceId, final String userId, final String sessionId) {
        return tokenManager.get()
                .generateTokenForSession(serviceId, sessionId, TokenType.STATIC)
                .filter(generatedTokenInfo -> generatedTokenInfo.getUser().getUser().getId().equals(userId))
                .map(generatedTokenInfo -> (EngineEvalResult) new ViewOpSuccess(
                        new TokenView(generatedTokenInfo.getToken(), generatedTokenInfo.getUser().getUser().getId())))
                .orElse(new GeneralOpFailure());
    }

    public EngineEvalResult deleteToken(
            ServiceUserPrincipal principal,
            String serviceId,
            String userId,
            String sessionId,
            TokenType type) {
        return sessionStore.get()
                       .get(sessionId, type)
                       .filter(session -> principal.getServiceUser().getRole().equals(IdmanRoles.ADMIN)
                               || (session.getUserId().equals(userId) && session.getServiceId().equals(serviceId)))
                       .map(session -> sessionStore.get().delete(session.getSessionId(), type))
                       .orElse(Boolean.FALSE)
               ? new UserOpSuccess(userId)
               : new UserOpFailure(userId);
    }
}
