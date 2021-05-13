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

package io.appform.idman.server.localauth;

import com.google.common.base.Strings;
import io.appform.idman.model.AuthMode;
import io.appform.idman.model.UserType;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.impl.PasswordAuthInfo;
import io.appform.idman.server.auth.impl.PasswordAuthenticationProvider;
import io.appform.idman.server.db.*;
import io.appform.idman.server.db.impl.*;
import io.appform.idman.server.db.model.*;
import io.appform.idman.server.utils.TestingUtils;
import io.appform.idman.server.utils.Utils;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class LocalIdmanAuthClientTest {
    public DAOTestExtension db = DAOTestExtension.newBuilder()
            .addEntityClass(StoredUser.class)
            .addEntityClass(StoredUserAuthState.class)
            .addEntityClass(StoredPassword.class)
            .addEntityClass(StoredUserSession.class)
            .addEntityClass(StoredService.class)
            .addEntityClass(StoredRole.class)
            .addEntityClass(StoredUserRole.class)
            .build();

    private UserInfoStore userStore;
    private SessionStore sessionStore;
    private RoleStore roleStore;
    private UserRoleStore userRoleStore;
    private ServiceStore serviceStore;
    private PasswordStore passwordStore;
    private final AuthenticationConfig config = TestingUtils.passwordauthConfig();

    private LocalIdmanAuthClient client;

    @BeforeEach
    void setup() {
        userStore = new DBUserInfoStore(db.getSessionFactory());
        sessionStore = new DBSessionStore(db.getSessionFactory());
        roleStore = new DBRoleStore(db.getSessionFactory());
        userRoleStore = new DBUserRoleStore(db.getSessionFactory());
        serviceStore = new DBServiceStore(db.getSessionFactory());
        passwordStore = new DBPasswordStore(db.getSessionFactory());
        client = new LocalIdmanAuthClient(sessionStore, userStore, serviceStore, userRoleStore, config);
    }

    @Test
    void testValidate() {
        val user = db.inTransaction(() -> userStore.create("U1", "u@u.t", "TestUser", UserType.HUMAN, AuthMode.PASSWORD)
                .orElse(null));
        assertNotNull(user);
        db.inTransaction(() -> passwordStore.set(user.getUserId(), "PASSWORD"));
        val service = db.inTransaction(() -> serviceStore.create("S1", "Test Service", "http://localhost:8080"))
                .orElse(null);
        assertNotNull(service);
        val role = db.inTransaction(() -> roleStore.create(service.getServiceId(), "TestRole", "")).orElse(null);
        assertNotNull(role);
        db.inTransaction(() -> userRoleStore.mapUserToRole(user.getUserId(),
                                                           service.getServiceId(),
                                                           role.getRoleId(),
                                                           "TEST"));
        val authProvider = new PasswordAuthenticationProvider(config,
                                                              () -> userStore,
                                                              () -> passwordStore,
                                                              () -> sessionStore);
        val session = db.inTransaction(() -> authProvider.login(
                new PasswordAuthInfo("u@u.t", "PASSWORD", service.getServiceId(), "CS1"), "S1"))
                .orElse(null);
        assertNotNull(session);
        val jwt = Utils.createJWT(session, config.getJwt());
        assertFalse(Strings.isNullOrEmpty(jwt));
        val idmanUser = db.inTransaction(() -> client.validateImpl(jwt, service.getServiceId()));
        assertNotNull(idmanUser);
        assertEquals(role.getRoleId(), idmanUser.getRole());
    }

    @Test
    void testValidateDeletedService() {
        val user = db.inTransaction(() -> userStore.create("U1", "u@u.t", "TestUser", UserType.HUMAN, AuthMode.PASSWORD)
                .orElse(null));
        assertNotNull(user);
        db.inTransaction(() -> passwordStore.set(user.getUserId(), "PASSWORD"));
        val service = db.inTransaction(() -> serviceStore.create("S1", "Test Service", "http://localhost:8080"))
                .orElse(null);
        assertNotNull(service);
        val authProvider = new PasswordAuthenticationProvider(config,
                                                              () -> userStore,
                                                              () -> passwordStore,
                                                              () -> sessionStore);
        val session = db.inTransaction(() -> authProvider.login(
                new PasswordAuthInfo("u@u.t", "PASSWORD", service.getServiceId(), "CS1"), "S1"))
                .orElse(null);
        assertNotNull(session);
        val jwt = Utils.createJWT(session, config.getJwt());
        assertFalse(Strings.isNullOrEmpty(jwt));
        val idmanUser = db.inTransaction(() -> client.validateImpl(jwt, service.getServiceId()));
        assertNotNull(idmanUser);
        assertTrue(db.inTransaction(() -> serviceStore.delete(service.getServiceId())));
        assertNull(db.inTransaction(() -> client.validateImpl(jwt, "S2")));
        assertNull(db.inTransaction(() -> client.validateImpl(jwt, service.getServiceId())));
    }

    @Test
    void testMalformedToken() {
        val service = db.inTransaction(() -> serviceStore.create("S1", "Test Service", "http://localhost:8080"))
                .orElse(null);
        assertNotNull(service);
        assertNull(db.inTransaction(() -> client.validateImpl("abc", "S1")));
    }

    @Test
    void testValidateWrongService() {
        val user = db.inTransaction(() -> userStore.create("U1", "u@u.t", "TestUser", UserType.HUMAN, AuthMode.PASSWORD)
                .orElse(null));
        assertNotNull(user);
        db.inTransaction(() -> passwordStore.set(user.getUserId(), "PASSWORD"));
        val service = db.inTransaction(() -> serviceStore.create("S1", "Test Service", "http://localhost:8080"))
                .orElse(null);
        assertNotNull(service);
        db.inTransaction(() -> serviceStore.create("S2", "Test Service 2", "http://localhost:8080"));
        val authProvider = new PasswordAuthenticationProvider(config,
                                                              () -> userStore,
                                                              () -> passwordStore,
                                                              () -> sessionStore);
        val session = db.inTransaction(() -> authProvider.login(
                new PasswordAuthInfo("u@u.t", "PASSWORD", service.getServiceId(), "CS1"), "S1"))
                .orElse(null);
        assertNotNull(session);
        val jwt = Utils.createJWT(session, config.getJwt());
        assertFalse(Strings.isNullOrEmpty(jwt));
        val idmanUser = db.inTransaction(() -> client.validateImpl(jwt, service.getServiceId()));
        assertNotNull(idmanUser);
        assertNull(db.inTransaction(() -> client.validateImpl(jwt, "S2")));
    }

    @Test
    void testValidateNoSession() {
        val user = db.inTransaction(() -> userStore.create("U1", "u@u.t", "TestUser", UserType.HUMAN, AuthMode.PASSWORD)
                .orElse(null));
        assertNotNull(user);
        db.inTransaction(() -> passwordStore.set(user.getUserId(), "PASSWORD"));
        val service = db.inTransaction(() -> serviceStore.create("S1", "Test Service", "http://localhost:8080"))
                .orElse(null);
        assertNotNull(service);
        val authProvider = new PasswordAuthenticationProvider(config,
                                                              () -> userStore,
                                                              () -> passwordStore,
                                                              () -> sessionStore);
        val session = db.inTransaction(() -> authProvider.login(
                new PasswordAuthInfo("u@u.t", "PASSWORD", service.getServiceId(), "CS1"), "S1"))
                .orElse(null);
        assertNotNull(session);
        val jwt = Utils.createJWT(session, config.getJwt());
        assertFalse(Strings.isNullOrEmpty(jwt));
        db.inTransaction(() -> sessionStore.delete(session.getSessionId()));
        assertNull(db.inTransaction(() -> client.validateImpl(jwt, service.getServiceId())));
    }

    @Test
    void testValidateDeletedUser() {
        val user = db.inTransaction(() -> userStore.create("U1", "u@u.t", "TestUser", UserType.HUMAN, AuthMode.PASSWORD)
                .orElse(null));
        assertNotNull(user);
        db.inTransaction(() -> passwordStore.set(user.getUserId(), "PASSWORD"));
        val service = db.inTransaction(() -> serviceStore.create("S1", "Test Service", "http://localhost:8080"))
                .orElse(null);
        assertNotNull(service);
        val authProvider = new PasswordAuthenticationProvider(config,
                                                              () -> userStore,
                                                              () -> passwordStore,
                                                              () -> sessionStore);
        val session = db.inTransaction(() -> authProvider.login(
                new PasswordAuthInfo("u@u.t", "PASSWORD", service.getServiceId(), "CS1"), "S1"))
                .orElse(null);
        assertNotNull(session);
        val jwt = Utils.createJWT(session, config.getJwt());
        assertFalse(Strings.isNullOrEmpty(jwt));
        val idmanUser = db.inTransaction(() -> client.validateImpl(jwt, service.getServiceId()));
        assertNotNull(idmanUser);
        db.inTransaction(() -> userStore.deleteUser(user.getUserId()));
        assertNull(db.inTransaction(() -> client.validateImpl(jwt, service.getServiceId())));
        db.inTransaction(() -> db.getSessionFactory().getCurrentSession()
                .createSQLQuery("delete from users where user_id = 'U1'")
                .executeUpdate());
        assertNull(db.inTransaction(() -> client.validateImpl(jwt, service.getServiceId())));
    }

}