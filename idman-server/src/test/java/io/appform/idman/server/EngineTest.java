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

import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.model.*;
import io.appform.idman.server.auth.IdmanRoles;
import io.appform.idman.server.auth.TokenManager;
import io.appform.idman.server.db.*;
import io.appform.idman.server.db.model.*;
import io.appform.idman.server.engine.Engine;
import io.appform.idman.server.engine.results.*;
import io.appform.idman.server.utils.ServerTestingUtils;
import io.appform.idman.server.views.PasswordChangeView;
import io.appform.idman.server.views.ServiceDetailsView;
import io.appform.idman.server.views.UserDetailsView;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static io.appform.idman.server.utils.Utils.toWire;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class EngineTest {

    private ServiceStore serviceStore = mock(ServiceStore.class);
    private RoleStore roleStore = mock(RoleStore.class);
    private UserInfoStore userInfoStore = mock(UserInfoStore.class);
    private PasswordStore passwordStore = mock(PasswordStore.class);
    private UserRoleStore userRoleStore = mock(UserRoleStore.class);
    private SessionStore sessionStore = mock(SessionStore.class);
    private TokenManager tokenManager = mock(TokenManager.class);

    private Engine engine = new Engine(() -> serviceStore,
                                       () -> roleStore,
                                       () -> userInfoStore,
                                       () -> passwordStore,
                                       () -> userRoleStore,
                                       () -> sessionStore,
                                       () -> tokenManager);

    @AfterEach
    void destroy() {
        reset(serviceStore,
              roleStore,
              passwordStore,
              userInfoStore,
              userRoleStore,
              sessionStore);
    }

    @Test
    void renderHomeRedirectSuccess() {
        val storedUser = ServerTestingUtils.adminUser();
        val user = toWire(storedUser);
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", user, IdmanRoles.ADMIN));

        val r = engine.renderHome(principal, "/");
        assertEquals(RedirectToParam.class, r.getClass());
        assertEquals("/", ((RedirectToParam) r).getRedirect());
    }

    @Test
    void renderHomeRenderSuccess() {

        val storedUser = ServerTestingUtils.adminUser();

        val user = toWire(storedUser);
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", user, IdmanRoles.ADMIN));
        ServerTestingUtils.runInCtx(() -> {
            val r = engine.renderHome(principal, null);
            assertEquals(ViewOpSuccess.class, r.getClass());
        });
    }

    @Test
    void renderHomeExpiredUser() {

        val storedUser = ServerTestingUtils.adminUser();

        val user = toWire(storedUser);
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", user, IdmanRoles.ADMIN));

        storedUser.getAuthState().setAuthState(AuthState.EXPIRED);
        val r = engine.renderHome(principal, "/");
        assertEquals(CredentialsExpired.class, r.getClass());
        assertEquals(user.getId(), ((CredentialsExpired) r).getUserId());
    }

    @Test
    void renderHomeDeletedUser() {

        val storedUser = ServerTestingUtils.adminUser();

        val user = toWire(storedUser);
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", user, IdmanRoles.ADMIN));
        storedUser.setDeleted(true);
        val r = engine.renderHome(principal, "/");
        assertEquals(InvalidUser.class, r.getClass());
    }

    @Test
    void renderHomeInvalidPrincipal() {
        doReturn(Optional.empty()).when(userInfoStore).get(anyString());
        val invalidPrincipal = new ServiceUserPrincipal(idmanUser("Ts1",
                                                                  "S1",
                                                                  "blah",
                                                                  "",
                                                                  UserType.HUMAN,
                                                                  AuthMode.PASSWORD,
                                                                  IdmanRoles.ADMIN));
        val r = engine.renderHome(invalidPrincipal, "/");
        assertEquals(InvalidUser.class, r.getClass());
    }

    @Test
    void createServiceFail() {
        doReturn(Optional.empty())
                .when(serviceStore).create(anyString(), anyString(), anyString());
        val r = engine.createService("S", "S", "x.com");
        assertEquals(InvalidService.class, r.getClass());
    }

    @Test
    void createServiceSuccess() {
        doReturn(Optional.of(ServerTestingUtils.testService()))
                .when(serviceStore).create(anyString(), anyString(), anyString());
        val r = engine.createService("S", "S", "x.com");
        assertEquals(ServiceOpSuccess.class, r.getClass());
        assertEquals("S", ((ServiceOpSuccess) r).getServiceId());
    }

    @Test
    void renderServiceDetailsInvalidServiceId() {
        doReturn(Optional.empty()).when(serviceStore).get(anyString());
        val storedUser = ServerTestingUtils.adminUser();

        val user = toWire(storedUser);
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", user, IdmanRoles.ADMIN));
        val r = engine.renderServiceDetails(principal, "S");
        assertEquals(InvalidService.class, r.getClass());
    }

    @Test
    void renderServiceDetailsSuccess() {
        val storedUser = ServerTestingUtils.adminUser();

        val user = toWire(storedUser);
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", user, IdmanRoles.ADMIN));

        val testService = ServerTestingUtils.testService();
        setupServiceMock(testService);
        doReturn(List.of(
                new StoredUserRole(storedUser.getUserId(), testService.getServiceId(), "S_ADMIN", "TEST"),
                new StoredUserRole(storedUser.getUserId(), testService.getServiceId(), "S_ADMIN_1", "TEST"),
                new StoredUserRole("blah", testService.getServiceId(), "S_ADMIN", "TEST")
                        ))
                .when(userRoleStore).getServiceRoleMappings(testService.getServiceId());
        doReturn(Collections.singletonList(storedUser)).when(userInfoStore).list(false);
        doReturn(Collections.singletonList(adminRole()))
                .when(roleStore).list("S", false);
        ServerTestingUtils.runInCtx(() -> {
            val r = engine.renderServiceDetails(principal, "S");
            assertEquals(ViewOpSuccess.class, r.getClass());
            val v = ((ViewOpSuccess) r).getView();
            assertEquals(ServiceDetailsView.class, v.getClass());
            assertEquals(1, ((ServiceDetailsView) v).getMappedUsers().size());
        });
    }

    @Test
    void updateServiceDescriptionFailure() {
        doReturn(Optional.empty()).when(serviceStore).updateDescription(anyString(), anyString());

        val r = engine.updateServiceDescription("S", "test");
        assertEquals(InvalidService.class, r.getClass());
    }

    @Test
    void updateServiceDescriptionSuccess() {
        val testService = ServerTestingUtils.testService();
        doReturn(Optional.of(testService)).when(serviceStore).updateDescription("S", "test");

        val r = engine.updateServiceDescription("S", "test");
        assertEquals(ServiceOpSuccess.class, r.getClass());
        assertEquals("S", ((ServiceOpSuccess) r).getServiceId());
    }

    @Test
    void updateServiceCallbackUrlFailure() {
        doReturn(Optional.empty()).when(serviceStore).updateCallbackUrl(anyString(), anyString());

        val r = engine.updateServiceCallbackUrl("S", "test");
        assertEquals(InvalidService.class, r.getClass());
    }

    @Test
    void updateServiceCallbackUrlSuccess() {
        val testService = ServerTestingUtils.testService();
        doReturn(Optional.of(testService)).when(serviceStore).updateCallbackUrl("S", "test.com");

        val r = engine.updateServiceCallbackUrl("S", "test.com");
        assertEquals(ServiceOpSuccess.class, r.getClass());
        assertEquals("S", ((ServiceOpSuccess) r).getServiceId());
    }

    @Test
    void regenerateServiceSecretFailure() {
        doReturn(Optional.empty()).when(serviceStore).updateSecret(anyString());
        val r = engine.regenerateServiceSecret("S");
        assertEquals(InvalidService.class, r.getClass());
    }

    @Test
    void regenerateServiceSecretSuccess() {
        val testService = ServerTestingUtils.testService();
        doReturn(Optional.of(testService)).when(serviceStore).updateSecret("S");

        val r = engine.regenerateServiceSecret("S");
        assertEquals(ServiceOpSuccess.class, r.getClass());
        assertEquals(testService.getServiceId(), ((ServiceOpSuccess) r).getServiceId());
    }

    @Test
    void deleteServiceFailure() {
        doReturn(false).when(serviceStore).delete(anyString());
        val r = engine.deleteService("S");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void deleteServiceSuccess() {
        doReturn(true).when(serviceStore).delete("S");
        val r = engine.deleteService("S");
        assertEquals(GeneralOpSuccess.class, r.getClass());
    }

    @Test
    void createRoleFailBadService() {
        doReturn(Optional.empty()).when(serviceStore).get(anyString());
        val r = engine.createRole("S", "ADMIN", "Admin role");
        assertEquals(InvalidService.class, r.getClass());
    }

    @Test
    void createRoleFailure() {
        val testService = ServerTestingUtils.testService();
        setupServiceMock(testService);
        doReturn(Optional.empty())
                .when(roleStore).create(anyString(), anyString(), anyString());
        val r = engine.createRole(testService.getServiceId(), "ADMIN", "Admin");
        assertEquals(RoleOpFailure.class, r.getClass());
        assertEquals(testService.getServiceId(), ((RoleOpFailure) r).getServiceId());
        assertNull(((RoleOpFailure) r).getRoleId());
    }

    @Test
    void createRoleSuccess() {
        val testService = ServerTestingUtils.testService();
        setupServiceMock(testService);
        val adminRole = adminRole();
        doReturn(Optional.of(adminRole))
                .when(roleStore).create(testService.getServiceId(), "ADMIN", "Admin");
        val r = engine.createRole(testService.getServiceId(), "ADMIN", "Admin");
        assertEquals(RoleOpSuccess.class, r.getClass());
        assertEquals(testService.getServiceId(), ((RoleOpSuccess) r).getServiceId());
        assertEquals(adminRole.getRoleId(), ((RoleOpSuccess) r).getRoleId());
    }

    @Test
    void updateRoleFailBadService() {
        doReturn(Optional.empty()).when(serviceStore).get(anyString());
        val r = engine.updateRole("S", "ADMIN", "Admin role 1");
        assertEquals(InvalidService.class, r.getClass());
    }

    @Test
    void updateRoleFailure() {
        val testService = ServerTestingUtils.testService();
        setupServiceMock(testService);
        doReturn(Optional.empty())
                .when(roleStore).update(anyString(), anyString(), anyString());
        val r = engine.updateRole(testService.getServiceId(), "S_ADMIN", "Admin");
        assertEquals(RoleOpFailure.class, r.getClass());
        assertEquals(testService.getServiceId(), ((RoleOpFailure) r).getServiceId());
        assertEquals("S_ADMIN", ((RoleOpFailure) r).getRoleId());
    }

    @Test
    void updateRoleSuccess() {
        val testService = ServerTestingUtils.testService();
        setupServiceMock(testService);
        val adminRole = adminRole();
        doReturn(Optional.of(adminRole))
                .when(roleStore).update(testService.getServiceId(), "S_ADMIN", "Admin");
        val r = engine.updateRole(testService.getServiceId(), "S_ADMIN", "Admin");
        assertEquals(RoleOpSuccess.class, r.getClass());
        assertEquals(testService.getServiceId(), ((RoleOpSuccess) r).getServiceId());
        assertEquals(adminRole.getRoleId(), ((RoleOpSuccess) r).getRoleId());
    }


    @Test
    void deleteRoleInvalidService() {
        doReturn(Optional.empty()).when(serviceStore).get(anyString());
        val r = engine.deleteRole("S", "S_ADMIN");
        assertEquals(InvalidService.class, r.getClass());
    }

    @Test
    void deleteRoleFailure() {
        val testService = ServerTestingUtils.testService();
        setupServiceMock(testService);
        doReturn(false).when(roleStore).delete(anyString(), anyString());
        val r = engine.deleteRole(testService.getServiceId(), "S_ADMIN");
        assertEquals(RoleOpFailure.class, r.getClass());
        assertEquals(testService.getServiceId(), ((RoleOpFailure) r).getServiceId());
        assertEquals("S_ADMIN", ((RoleOpFailure) r).getRoleId());
    }

    @Test
    void deleteRoleSuccess() {
        val testService = ServerTestingUtils.testService();
        setupServiceMock(testService);
        doReturn(true).when(roleStore).delete("S", "S_ADMIN");
        val r = engine.deleteRole(testService.getServiceId(), "S_ADMIN");
        assertEquals(RoleOpSuccess.class, r.getClass());
        assertEquals(testService.getServiceId(), ((RoleOpSuccess) r).getServiceId());
        assertEquals("S_ADMIN", ((RoleOpSuccess) r).getRoleId());
    }

    @Test
    void createHumanUserFailure() {
        doReturn(Optional.empty()).when(userInfoStore).create(anyString(),
                                                              anyString(),
                                                              anyString(),
                                                              eq(UserType.HUMAN),
                                                              eq(AuthMode.PASSWORD));
        val r = engine.createHumanUser("a@a.com", "A", "blah");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void createHumanUserSuccess() {
        val user = ServerTestingUtils.adminUser();
        doReturn(Optional.of(user)).when(userInfoStore).create(user.getUserId(),
                                                               user.getEmail(),
                                                               user.getName(),
                                                               UserType.HUMAN,
                                                               AuthMode.PASSWORD);
        val r = engine.createHumanUser(user.getEmail(), user.getName(), "blah");
        assertEquals(UserOpSuccess.class, r.getClass());
        assertEquals(user.getUserId(), ((UserOpSuccess) r).getUserId());
    }

    @Test
    void createSystemUserSuccess() {
        val user = ServerTestingUtils.systemUser();
        doReturn(Optional.of(user)).when(userInfoStore)
                .create(user.getUserId(), user.getEmail(), user.getName(), UserType.SYSTEM, AuthMode.TOKEN, false);
        val r = engine.createSystemUser(user.getEmail(), user.getName());
        assertEquals(UserOpSuccess.class, r.getClass());
        assertEquals(user.getUserId(), ((UserOpSuccess) r).getUserId());
    }

    @Test
    void createSystemUserFailure() {
        val user = ServerTestingUtils.systemUser();
        doReturn(Optional.empty()).when(userInfoStore)
                .create(user.getUserId(), user.getEmail(), user.getName(), UserType.SYSTEM, AuthMode.TOKEN, false);
        val r = engine.createSystemUser(user.getEmail(), user.getName());
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void userDetailsInvalidUser() {
        doReturn(Optional.empty()).when(userInfoStore).get(anyString());

        val r = engine.userDetails(null, "u1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void userDetailsSuccess() {
        val testService = ServerTestingUtils.testService();
        val storedUser = ServerTestingUtils.adminUser();
        val user = toWire(storedUser);

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", user, IdmanRoles.ADMIN));
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        doReturn(Collections.singletonList(testService)).when(serviceStore).get(anyCollection());
        doReturn(List.of(
                new StoredUserRole(storedUser.getUserId(), testService.getServiceId(), "S_ADMIN", "TEST"),
                new StoredUserRole(storedUser.getUserId(), "S1", "S_ADMIN", "TEST"),
                new StoredUserRole(storedUser.getUserId(), testService.getServiceId(), "S_ADMIN_1", "TEST")))
                .when(userRoleStore)
                .getUserRoles(storedUser.getUserId());
        doReturn(Collections.singletonList(new StoredRole("S_ADMIN", testService.getServiceId(), "Admin", "test")))
                .when(roleStore).get(anyCollection());
        doReturn(List.of(
                new ClientSession("S1",
                                  storedUser.getUserId(),
                                  testService.getServiceId(),
                                  "CS1",
                                  TokenType.DYNAMIC,
                                  new Date(),
                                  false,
                                  null,
                                  null),
                new ClientSession("S2",
                                  storedUser.getUserId(),
                                  "S1",
                                  "CS1",
                                  TokenType.DYNAMIC,
                                  new Date(),
                                  false,
                                  null,
                                  null)))
                .when(sessionStore)
                .sessionsForUser(user.getId(), TokenType.DYNAMIC);

        ServerTestingUtils.runInCtx(() -> {
            val r = engine.userDetails(principal, storedUser.getUserId());
            assertEquals(ViewOpSuccess.class, r.getClass());
            val v = (UserDetailsView) ((ViewOpSuccess) r).getView();
            assertEquals(storedUser, v.getUser());
            assertEquals(1, v.getServices().size());
        });
    }

    @Test
    void userDetailsStaticSuccess() {
        val testService = ServerTestingUtils.testService();
        val storedUser = ServerTestingUtils.systemUser();
        val user = toWire(storedUser);

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", user, IdmanRoles.ADMIN));
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        doReturn(Collections.singletonList(testService)).when(serviceStore).get(anyCollection());
        doReturn(List.of(
                new StoredUserRole(storedUser.getUserId(), testService.getServiceId(), "S_ADMIN", "TEST"),
                new StoredUserRole(storedUser.getUserId(), "S1", "S_ADMIN", "TEST"),
                new StoredUserRole(storedUser.getUserId(), testService.getServiceId(), "S_ADMIN_1", "TEST")))
                .when(userRoleStore)
                .getUserRoles(storedUser.getUserId());
        doReturn(Collections.singletonList(new StoredRole("S_ADMIN", testService.getServiceId(), "Admin", "test")))
                .when(roleStore).get(anyCollection());
        doReturn(List.of(
                new ClientSession("S1",
                                  storedUser.getUserId(),
                                  testService.getServiceId(),
                                  null,
                                  TokenType.STATIC,
                                  null,
                                  false,
                                  null,
                                  null),
                new ClientSession("S2",
                                  storedUser.getUserId(),
                                  "S1",
                                  null,
                                  TokenType.STATIC,
                                  null,
                                  false,
                                  null,
                                  null)))
                .when(sessionStore)
                .sessionsForUser(user.getId(), TokenType.STATIC);

        ServerTestingUtils.runInCtx(() -> {
            val r = engine.userDetails(principal, storedUser.getUserId());
            assertEquals(ViewOpSuccess.class, r.getClass());
            val v = (UserDetailsView) ((ViewOpSuccess) r).getView();
            assertEquals(storedUser, v.getUser());
            assertEquals(1, v.getServices().size());
        });
    }

    @Test
    void updateUserInvalidUser() {
        doReturn(Optional.empty()).when(userInfoStore).get(anyString());
        val r = engine.updateUser(null, "U1", "U");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void updateUserNonAdminFailure() {
        val storedUser = ServerTestingUtils.adminUser();

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1",
                                                               "S1",
                                                               toWire(ServerTestingUtils.normalUser()),
                                                               IdmanRoles.USER));
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        val r = engine.updateUser(principal, storedUser.getUserId(), "Test1");
        assertEquals(UserOpFailure.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((UserOpFailure) r).getUserId());
    }

    @Test
    void updateUserAdminSuccess() {
        val storedUser = ServerTestingUtils.normalUser();

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1",
                                                               "S1",
                                                               toWire(ServerTestingUtils.adminUser()),
                                                               IdmanRoles.ADMIN));
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        doReturn(Optional.of(storedUser)).when(userInfoStore).updateName(storedUser.getUserId(), "Test1");

        val r = engine.updateUser(principal, storedUser.getUserId(), "Test1");
        assertEquals(UserOpSuccess.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((UserOpSuccess) r).getUserId());
    }

    @Test
    void updateUserSelfSuccess() {
        val storedUser = ServerTestingUtils.normalUser();
        val user = toWire(storedUser);

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", user, IdmanRoles.USER));
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        doReturn(Optional.of(storedUser)).when(userInfoStore).updateName(storedUser.getUserId(), "Test1");

        val r = engine.updateUser(principal, storedUser.getUserId(), "Test1");
        assertEquals(UserOpSuccess.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((UserOpSuccess) r).getUserId());
    }

    @Test
    void updateUserFailure() {
        val storedUser = ServerTestingUtils.normalUser();
        val user = toWire(storedUser);

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", user, IdmanRoles.USER));
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        doReturn(Optional.empty()).when(userInfoStore).updateName(storedUser.getUserId(), "Test1");

        val r = engine.updateUser(principal, storedUser.getUserId(), "Test1");
        assertEquals(UserOpFailure.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((UserOpFailure) r).getUserId());
    }

    @Test
    void deleteUserInvUser() {
        doReturn(Optional.empty()).when(userInfoStore).get(anyString());
        val r = engine.deleteUser("U1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void deleteUserSuccess() {
        val user = ServerTestingUtils.normalUser();
        doReturn(Optional.of(user)).when(userInfoStore).get(user.getUserId());
        doReturn(true).when(userInfoStore).deleteUser(anyString());
        val r = engine.deleteUser(user.getUserId());
        assertEquals(GeneralOpSuccess.class, r.getClass());
    }

    @Test
    void renderPasswordChangePageInvUser() {
        doReturn(Optional.empty()).when(userInfoStore).get(anyString());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1",
                                                               "S1",
                                                               toWire(ServerTestingUtils.adminUser()),
                                                               IdmanRoles.ADMIN));
        val r = engine.renderPasswordChangePage(principal, "U1");
        assertEquals(UserOpFailure.class, r.getClass());
        assertEquals("U1", ((UserOpFailure) r).getUserId());
    }

    @Test
    void renderPasswordChangePageAdminSuccess() {
        val storedUser = ServerTestingUtils.normalUser();

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1",
                                                               "S1",
                                                               toWire(ServerTestingUtils.adminUser()),
                                                               IdmanRoles.ADMIN));
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        ServerTestingUtils.runInCtx(() -> {
            val r = engine.renderPasswordChangePage(principal, storedUser.getUserId());
            assertEquals(ViewOpSuccess.class, r.getClass());
            val v = (PasswordChangeView) ((ViewOpSuccess) r).getView();
            assertTrue(v.isSkipOld());
            assertEquals(storedUser, v.getUser());
        });
    }

    @Test
    void renderPasswordChangePageSelfSuccess() {
        val storedUser = ServerTestingUtils.normalUser();
        val user = toWire(storedUser);

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", user, IdmanRoles.ADMIN));
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        ServerTestingUtils.runInCtx(() -> {
            val r = engine.renderPasswordChangePage(principal, storedUser.getUserId());
            assertEquals(ViewOpSuccess.class, r.getClass());
            val v = (PasswordChangeView) ((ViewOpSuccess) r).getView();
            assertFalse(v.isSkipOld());
            assertEquals(storedUser, v.getUser());
        });
    }

    @Test
    void renderPasswordChangePageNonAdminFailure() {
        val storedUser = ServerTestingUtils.normalUser();

        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1",
                                                               "S1",
                                                               toWire(ServerTestingUtils.adminUser()),
                                                               IdmanRoles.USER));
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        val r = engine.renderPasswordChangePage(principal, storedUser.getUserId());
        assertEquals(UserOpFailure.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((UserOpFailure) r).getUserId());
    }

    @Test
    void changePasswordInvUser() {
        doReturn(Optional.empty()).when(userInfoStore).get(anyString());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1",
                                                               "S1",
                                                               toWire(ServerTestingUtils.adminUser()),
                                                               IdmanRoles.ADMIN));
        val r = engine.changePassword(principal, "U1", "xx", "yy", "yy");
        assertEquals(CredentialsExpired.class, r.getClass());
        assertEquals("U1", ((CredentialsExpired) r).getUserId());
    }

    @Test
    void changePasswordIdMismatch() {
        val storedUser = ServerTestingUtils.normalUser();
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1",
                                                               "S1",
                                                               toWire(ServerTestingUtils.adminUser()),
                                                               IdmanRoles.USER));
        val r = engine.changePassword(principal, storedUser.getUserId(), "xx", "yy", "yy");
        assertEquals(CredentialsExpired.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((CredentialsExpired) r).getUserId());
    }

    @Test
    void changePasswordConfMismatch() {
        val storedUser = ServerTestingUtils.normalUser();
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", toWire(storedUser), IdmanRoles.USER));
        val r = engine.changePassword(principal, storedUser.getUserId(), "xx", "yy", "yy1");
        assertEquals(CredentialsExpired.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((CredentialsExpired) r).getUserId());
    }

    @Test
    void changePasswordOldNewMismatch() {
        val storedUser = ServerTestingUtils.normalUser();
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", toWire(storedUser), IdmanRoles.USER));
        val r = engine.changePassword(principal, storedUser.getUserId(), "xx", "xx", "xx");
        assertEquals(CredentialsExpired.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((CredentialsExpired) r).getUserId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void changePasswordSelfSuccess() {
        val storedUser = ServerTestingUtils.normalUser();
        storedUser.getAuthState().setAuthState(AuthState.EXPIRED);
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", toWire(storedUser), IdmanRoles.USER));
        doReturn(true).when(passwordStore).update(storedUser.getUserId(), "xx", "yy");
        doAnswer((Answer<Optional<StoredUser>>) invocationOnMock -> {
            val consumer = (Consumer<StoredUserAuthState>) invocationOnMock.getArgument(1);
            consumer.accept(storedUser.getAuthState());
            return Optional.of(storedUser);
        }).when(userInfoStore).updateAuthState(eq(storedUser.getUserId()), any(Consumer.class));
        assertEquals(AuthState.EXPIRED, storedUser.getAuthState().getAuthState());
        val r = engine.changePassword(principal, storedUser.getUserId(), "xx", "yy", "yy");
        assertEquals(UserOpSuccess.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((UserOpSuccess) r).getUserId());
        assertEquals(AuthState.ACTIVE, storedUser.getAuthState().getAuthState());
    }

    @Test
    void changePasswordSelfFail() {
        val storedUser = ServerTestingUtils.normalUser();
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", toWire(storedUser), IdmanRoles.USER));
        doReturn(false).when(passwordStore).update(storedUser.getUserId(), "xx", "yy");
        val r = engine.changePassword(principal, storedUser.getUserId(), "xx", "yy", "yy");
        assertEquals(CredentialsExpired.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((CredentialsExpired) r).getUserId());
    }

    @Test
    void changePasswordForcedInvUser() {
        doReturn(Optional.empty()).when(userInfoStore).get(anyString());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1",
                                                               "S1",
                                                               toWire(ServerTestingUtils.adminUser()),
                                                               IdmanRoles.ADMIN));
        val r = engine.changePasswordForced(principal, "U1", "yy", "yy");
        assertEquals(UserOpFailure.class, r.getClass());
        assertEquals("U1", ((UserOpFailure) r).getUserId());
    }

    @Test
    void changePasswordForcedSelfFail() {
        val storedUser = ServerTestingUtils.normalUser();
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1", "S1", toWire(storedUser), IdmanRoles.USER));
        val r = engine.changePasswordForced(principal, storedUser.getUserId(), "yy", "yy");
        assertEquals(UserOpFailure.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((UserOpFailure) r).getUserId());
    }

    @Test
    void changePasswordForcedNonAdminFail() {
        val storedUser = ServerTestingUtils.normalUser();
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1",
                                                               "S1",
                                                               toWire(ServerTestingUtils.adminUser()),
                                                               IdmanRoles.USER));
        val r = engine.changePasswordForced(principal, storedUser.getUserId(), "yy", "yy");
        assertEquals(UserOpFailure.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((UserOpFailure) r).getUserId());
    }

    @Test
    void changePasswordForcedConfMismatchFail() {
        val storedUser = ServerTestingUtils.normalUser();
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1",
                                                               "S1",
                                                               toWire(ServerTestingUtils.adminUser()),
                                                               IdmanRoles.ADMIN));
        val r = engine.changePasswordForced(principal, storedUser.getUserId(), "yy", "yy1");
        assertEquals(UserOpFailure.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((UserOpFailure) r).getUserId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void changePasswordForcedAdminSuccess() {
        val storedUser = ServerTestingUtils.normalUser();
        doReturn(Optional.of(storedUser)).when(userInfoStore).get(storedUser.getUserId());
        val principal = new ServiceUserPrincipal(new IdmanUser("Ts1",
                                                               "S1",
                                                               toWire(ServerTestingUtils.adminUser()),
                                                               IdmanRoles.ADMIN));
        doNothing().when(passwordStore).set(storedUser.getUserId(), "xx");
        doAnswer((Answer<Optional<StoredUser>>) invocationOnMock -> {
            val consumer = (Consumer<StoredUserAuthState>) invocationOnMock.getArgument(1);
            consumer.accept(storedUser.getAuthState());
            return Optional.of(storedUser);
        }).when(userInfoStore).updateAuthState(eq(storedUser.getUserId()), any(Consumer.class));
        assertEquals(AuthState.ACTIVE, storedUser.getAuthState().getAuthState());
        val r = engine.changePasswordForced(principal, storedUser.getUserId(), "xx", "xx");
        assertEquals(UserOpSuccess.class, r.getClass());
        assertEquals(storedUser.getUserId(), ((UserOpSuccess) r).getUserId());
        assertEquals(AuthState.EXPIRED, storedUser.getAuthState().getAuthState());
    }

    @Test
    void mapUserToRoleInvService() {
        doReturn(Optional.empty()).when(serviceStore).get(anyString());
        val r = engine.mapUserToRole(null, URI.create("/role"), "S", "S_ADMIN", "U1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void mapUserToRoleDeletedService() {
        val service = ServerTestingUtils.testService();
        service.setDeleted(true);
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        val r = engine.mapUserToRole(null, URI.create("/role"), service.getServiceId(), "S_ADMIN", "U1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void mapUserToRoleInvRole() {
        val service = ServerTestingUtils.testService();
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.empty()).when(roleStore).get(eq(service.getServiceId()), anyString());
        val r = engine.mapUserToRole(null, URI.create("/role"), service.getServiceId(), "S_ADMIN", "U1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void mapUserToRoleDeletedRole() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();
        role.setDeleted(true);
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        val r = engine.mapUserToRole(null, URI.create("/role"), service.getServiceId(), "S_ADMIN", "U1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void mapUserToRoleInvalidUser() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();

        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        doReturn(Optional.empty()).when(userInfoStore).get(anyString());

        val r = engine.mapUserToRole(null, URI.create("/role"), service.getServiceId(), "S_ADMIN", "U1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void mapUserToRoleDeletedUser() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();
        val user = ServerTestingUtils.normalUser();
        user.setDeleted(true);
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        doReturn(Optional.of(user)).when(userInfoStore).get(user.getUserId());

        val r = engine.mapUserToRole(null, URI.create("/role"), service.getServiceId(), "S_ADMIN", user.getUserId());
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void mapUserToRoleSuccessRedirect() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();
        val user = ServerTestingUtils.normalUser();
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        doReturn(Optional.of(user)).when(userInfoStore).get(user.getUserId());

        val principal = new ServiceUserPrincipal(new IdmanUser("ts1", "IDMAN", toWire(user), IdmanRoles.ADMIN));
        val r = engine.mapUserToRole(principal,
                                     URI.create("/role"),
                                     service.getServiceId(),
                                     "S_ADMIN",
                                     user.getUserId());
        assertEquals(RedirectToParam.class, r.getClass());
    }

    @Test
    void mapUserToRoleSuccessNullRedirect() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();
        val user = ServerTestingUtils.normalUser();
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        doReturn(Optional.of(user)).when(userInfoStore).get(user.getUserId());

        val principal = new ServiceUserPrincipal(new IdmanUser("ts1", "IDMAN", toWire(user), IdmanRoles.ADMIN));
        val r = engine.mapUserToRole(principal, null, service.getServiceId(), "S_ADMIN", user.getUserId());
        assertEquals(UserOpSuccess.class, r.getClass());
    }

    @Test
    void mapUserToRoleSuccessEmptyRedirect() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();
        val user = ServerTestingUtils.normalUser();
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        doReturn(Optional.of(user)).when(userInfoStore).get(user.getUserId());

        val principal = new ServiceUserPrincipal(new IdmanUser("ts1", "IDMAN", toWire(user), IdmanRoles.ADMIN));
        val r = engine.mapUserToRole(principal, URI.create(""), service.getServiceId(), "S_ADMIN", user.getUserId());
        assertEquals(UserOpSuccess.class, r.getClass());
    }

    @Test
    void unmapUserToRoleInvService() {
        doReturn(Optional.empty()).when(serviceStore).get(anyString());
        val r = engine.unmapUserFromRole(URI.create("/role"), "S", "S_ADMIN", "U1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void unmapUserToRoleDeletedService() {
        val service = ServerTestingUtils.testService();
        service.setDeleted(true);
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        val r = engine.unmapUserFromRole(URI.create("/role"), service.getServiceId(), "S_ADMIN", "U1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void unmapUserToRoleInvRole() {
        val service = ServerTestingUtils.testService();
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.empty()).when(roleStore).get(eq(service.getServiceId()), anyString());
        val r = engine.unmapUserFromRole(URI.create("/role"), service.getServiceId(), "S_ADMIN", "U1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void unmapUserToRoleDeletedRole() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();
        role.setDeleted(true);
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        val r = engine.unmapUserFromRole(URI.create("/role"), service.getServiceId(), "S_ADMIN", "U1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void unmapUserToRoleInvalidUser() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();

        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        doReturn(Optional.empty()).when(userInfoStore).get(anyString());

        val r = engine.unmapUserFromRole(URI.create("/role"), service.getServiceId(), "S_ADMIN", "U1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void unmapUserToRoleDeletedUser() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();
        val user = ServerTestingUtils.normalUser();
        user.setDeleted(true);
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        doReturn(Optional.of(user)).when(userInfoStore).get(user.getUserId());

        val r = engine.unmapUserFromRole(URI.create("/role"), service.getServiceId(), "S_ADMIN", user.getUserId());
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void unmapUserToRoleSuccessRedirect() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();
        val user = ServerTestingUtils.normalUser();
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        doReturn(Optional.of(user)).when(userInfoStore).get(user.getUserId());

        val r = engine.unmapUserFromRole(URI.create("/role"), service.getServiceId(), "S_ADMIN", user.getUserId());
        assertEquals(RedirectToParam.class, r.getClass());
    }

    @Test
    void unmapUserToRoleSuccessNullRedirect() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();
        val user = ServerTestingUtils.normalUser();
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        doReturn(Optional.of(user)).when(userInfoStore).get(user.getUserId());

        val principal = new ServiceUserPrincipal(new IdmanUser("ts1", "IDMAN", toWire(user), IdmanRoles.ADMIN));
        val r = engine.unmapUserFromRole(null, service.getServiceId(), "S_ADMIN", user.getUserId());
        assertEquals(UserOpSuccess.class, r.getClass());
    }

    @Test
    void unmapUserToRoleSuccessEmptyRedirect() {
        val service = ServerTestingUtils.testService();
        val role = adminRole();
        val user = ServerTestingUtils.normalUser();
        doReturn(Optional.of(service)).when(serviceStore).get(anyString());
        doReturn(Optional.of(role)).when(roleStore).get(eq(service.getServiceId()), anyString());
        doReturn(Optional.of(user)).when(userInfoStore).get(user.getUserId());

        val principal = new ServiceUserPrincipal(new IdmanUser("ts1", "IDMAN", toWire(user), IdmanRoles.ADMIN));
        val r = engine.unmapUserFromRole(URI.create(""), service.getServiceId(), "S_ADMIN", user.getUserId());
        assertEquals(UserOpSuccess.class, r.getClass());
    }

    @Test
    void createTokenSuccess() {
        doReturn(Optional.of(ServerTestingUtils.staticSession()))
                .when(tokenManager)
                .createToken("S", "U1", null, TokenType.STATIC, null);
        val r = engine.createStaticSession("U1", "S");
        assertEquals(TokenOpSuccess.class, r.getClass());
    }

    @Test
    void createTokenFailure() {
        doReturn(Optional.empty())
                .when(tokenManager)
                .createToken("S", "U1", null, TokenType.STATIC, null);
        val r = engine.createStaticSession("U1", "S");
        assertEquals(UserOpFailure.class, r.getClass());
    }

    @Test
    void testViewTokenSuccess() {
        val user = storedToIdmanUser("SS1", "S", "S_USER", ServerTestingUtils.systemUser());
        val tokenInfo = new TokenManager.GeneratedTokenInfo(user.getServiceUser(), "AT1");
        doReturn(Optional.of(tokenInfo))
                .when(tokenManager)
                .generateTokenForSession("S", "SS1", TokenType.STATIC);
        ServerTestingUtils.runInCtx(() -> {
            val r = engine.viewToken("S", user.getServiceUser().getUser().getId(), "SS1");
            assertEquals(ViewOpSuccess.class, r.getClass());

        });
    }

    @Test
    void testViewTokenNoTokenFail() {
        doReturn(Optional.empty())
                .when(tokenManager)
                .generateTokenForSession("S", "SS1", TokenType.STATIC);
        val r = engine.viewToken("S", "WRONG", "SS1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }

    @Test
    void testViewTokenFail() {
        val user = ServerTestingUtils.systemUser();
        val tokenInfo = new TokenManager.GeneratedTokenInfo(
                idmanUser("SS1", "S", user.getUserId(), user.getName(), user.getUserType(), user.getAuthState()
                        .getAuthMode(), "S_USER"),
                "AT1");
        doReturn(Optional.of(tokenInfo))
                .when(tokenManager)
                .generateTokenForSession("S", "SS1", TokenType.STATIC);
        val r = engine.viewToken("S", "WRONG", "SS1");
        assertEquals(GeneralOpFailure.class, r.getClass());
    }


    @Test
    void testDeleteTokenSuccessAdmin() {
        doReturn(Optional.of(new ClientSession("SS1", "U1", "S", null, TokenType.STATIC, null, false, null, null)))
                .when(sessionStore)
                .get("SS1", TokenType.STATIC);
        doReturn(true)
                .when(sessionStore)
                .delete("SS1", TokenType.STATIC);

        val user = storedToIdmanUser("CS1", "S", IdmanRoles.ADMIN, ServerTestingUtils.adminUser());
        val r = engine.deleteToken(user, "S", "U1", "SS1", TokenType.STATIC);
        assertEquals(UserOpSuccess.class, r.getClass());
        assertEquals("U1", ((UserOpSuccess)r).getUserId());
    }

    @Test
    void testDeleteTokenSuccessSelf() {
        val normalUser = ServerTestingUtils.normalUser();
        doReturn(Optional.of(new ClientSession("SS1", normalUser.getUserId(), "S", null, TokenType.STATIC, null, false, null, null)))
                .when(sessionStore)
                .get("SS1", TokenType.STATIC);
        doReturn(true)
                .when(sessionStore)
                .delete("SS1", TokenType.STATIC);

        val user = storedToIdmanUser("CS1", "S", IdmanRoles.USER, normalUser);
        val r = engine.deleteToken(user, "S", normalUser.getUserId(), "SS1", TokenType.STATIC);
        assertEquals(UserOpSuccess.class, r.getClass());
        assertEquals(normalUser.getUserId(), ((UserOpSuccess)r).getUserId());
    }

    @Test
    void testDeleteTokenFailDifferentUser() {
        doReturn(Optional.of(new ClientSession("SS1", "U1", "S", null, TokenType.STATIC, null, false, null, null)))
                .when(sessionStore)
                .get("SS1", TokenType.STATIC);
        doReturn(true)
                .when(sessionStore)
                .delete("SS1", TokenType.STATIC);

        val user = storedToIdmanUser("CS1", "S", IdmanRoles.USER, ServerTestingUtils.normalUser());
        val r = engine.deleteToken(user, "S", "U2", "SS1", TokenType.STATIC);
        assertEquals(UserOpFailure.class, r.getClass());
    }


    @Test
    void testDeleteTokenFailDifferentService() {
        val normalUser = ServerTestingUtils.normalUser();
        doReturn(Optional.of(new ClientSession("SS1", normalUser.getUserId(), "S1", null, TokenType.STATIC, null, false, null, null)))
                .when(sessionStore)
                .get("SS1", TokenType.STATIC);
        doReturn(true)
                .when(sessionStore)
                .delete("SS1", TokenType.STATIC);

        val user = storedToIdmanUser("CS1", "S", IdmanRoles.USER, normalUser);
        val r = engine.deleteToken(user, "S", normalUser.getUserId(), "SS1", TokenType.STATIC);
        assertEquals(UserOpFailure.class, r.getClass());
        assertEquals(normalUser.getUserId(), ((UserOpFailure)r).getUserId());
    }

    @Test
    void testDeleteTokenNoSession() {
        doReturn(Optional.of(new ClientSession("SS1", "U1", "S", null, TokenType.STATIC, null, false, null, null)))
                .when(sessionStore)
                .get("SS1", TokenType.STATIC);
        doReturn(false)
                .when(sessionStore)
                .delete("SS1", TokenType.STATIC);

        val user = storedToIdmanUser("CS1", "S", IdmanRoles.USER, ServerTestingUtils.normalUser());
        val r = engine.deleteToken(user, "S", "U2", "SS1", TokenType.STATIC);
        assertEquals(UserOpFailure.class, r.getClass());
    }

    @Test
    void testDeleteTokenDeleteFail() {
        doReturn(Optional.empty())
                .when(sessionStore)
                .get("SS1", TokenType.STATIC);
        doReturn(true)
                .when(sessionStore)
                .delete("SS1", TokenType.STATIC);

        val user = storedToIdmanUser("CS1", "S", IdmanRoles.USER, ServerTestingUtils.normalUser());
        val r = engine.deleteToken(user, "S", "U2", "SS1", TokenType.STATIC);
        assertEquals(UserOpFailure.class, r.getClass());
    }

    private StoredRole adminRole() {
        return new StoredRole("S_ADMIN", "S", "Admin", "test");
    }

    private void setupServiceMock(StoredService testService) {
        doReturn(Optional.of(testService)).when(serviceStore).get("S");
    }

    private static ServiceUserPrincipal storedToIdmanUser(
            String sessionId,
            String serviceId,
            String role,
            StoredUser user) {
        return new ServiceUserPrincipal(idmanUser(sessionId,
                         serviceId,
                         user.getUserId(),
                         user.getName(),
                         user.getUserType(),
                         user.getAuthState().getAuthMode(),
                         role));
    }

    private static IdmanUser idmanUser(
            String sessionId,
            String serviceId,
            String userId,
            String name,
            UserType userType,
            AuthMode authMode,
            String role) {
        return new IdmanUser(
                sessionId, serviceId,
                new User(userId,
                         name,
                         userType,
                         authMode),
                role);
    }
}