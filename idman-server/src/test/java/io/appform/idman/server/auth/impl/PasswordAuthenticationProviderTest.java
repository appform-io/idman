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

package io.appform.idman.server.auth.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.idman.model.AuthMode;
import io.appform.idman.model.UserType;
import io.appform.idman.server.auth.AuthenticationProviderFactory;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.db.*;
import io.appform.idman.server.db.impl.DBPasswordStore;
import io.appform.idman.server.db.impl.DBDynamicSessionStore;
import io.appform.idman.server.db.impl.DBStaticSessionStore;
import io.appform.idman.server.db.impl.DBUserInfoStore;
import io.appform.idman.server.db.model.*;
import io.appform.idman.server.utils.ServerTestingUtils;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Objects;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class PasswordAuthenticationProviderTest {

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(StoredUser.class)
            .addEntityClass(StoredUserAuthState.class)
            .addEntityClass(StoredPassword.class)
            .addEntityClass(StoredDynamicSession.class)
            .addEntityClass(StoredStaticSession.class)
            .build();

    private UserInfoStore userInfoStore;
    private PasswordStore passwordStore;
    private SessionStore sessionStore;
    private AuthenticationConfig authenticationConfig;

    @BeforeEach
    void setup() {
        userInfoStore = new DBUserInfoStore(database.getSessionFactory());
        passwordStore = new DBPasswordStore(database.getSessionFactory());
        sessionStore = new CompositeSessionStore(new DBDynamicSessionStore(database.getSessionFactory()),
                                                 new DBStaticSessionStore(database.getSessionFactory()));

        val user = database.inTransaction(() -> userInfoStore.create("UI",
                                                                     "u@u.t",
                                                                     "U",
                                                                     UserType.HUMAN,
                                                                     AuthMode.PASSWORD)).orElse(null);
        Objects.requireNonNull(user);
        database.inTransaction((Runnable) () -> passwordStore.set(user.getUserId(), "TESTPASSWORD"));

        authenticationConfig = ServerTestingUtils.passwordauthConfig();
    }

    @Test
    void testPasswordAuth() {
        val af = new AuthenticationProviderFactory(authenticationConfig,
                                                   new ObjectMapper(),
                                                   () -> userInfoStore,
                                                   () -> sessionStore,
                                                   () -> passwordStore);

        val authProvider = af.create(authenticationConfig.getProvider());
        val user = userInfoStore.get("UI").orElse(null);
        {
            assertNotNull(user);
            ClientSession resp = authProvider.login(new PasswordAuthInfo("u@u.t", "TESTPASSWORD", "S1", "CS1"), "S1")
                    .orElse(null);
            assertNotNull(resp);
        }
        {
            ClientSession resp = authProvider.login(new PasswordAuthInfo("u@u.t", "TESTPASSWORD1", "S1", "CS1"), "S1")
                    .orElse(null);
            assertNull(resp);
        }
        {
            try {
                authProvider.redirectionURL("xx");
                fail("Should have thrown unsupported operation exception");
            }
            catch (UnsupportedOperationException e) {
                assertEquals("This is not yet implemented", e.getMessage());
            }
        }
        {
            val authInfo = new GoogleAuthInfo("XX", "xx", "xx");
            try {
                authProvider.login(authInfo, "S1");
                fail("Expected illegal argument exception");
            }
            catch (IllegalArgumentException e) {
                assertEquals("Google auth info passed to password authenticator", e.getMessage());
            }

        }
    }

    @Test
    void testAccountLock() {
        val af = new AuthenticationProviderFactory(authenticationConfig,
                                                   new ObjectMapper(),
                                                   () -> userInfoStore,
                                                   () -> sessionStore,
                                                   () -> passwordStore);

        val authProvider = af.create(authenticationConfig.getProvider());
        val user = userInfoStore.get("UI").orElse(null);
        {
            assertNotNull(user);
            ClientSession resp = authProvider.login(new PasswordAuthInfo("u@u.t", "TESTPASSWORD", "S1", "CS1"), "S1")
                    .orElse(null);
            assertNotNull(resp);
        }
        IntStream.rangeClosed(1, 3)
                .forEach(i -> {
                    ClientSession resp = authProvider.login(new PasswordAuthInfo("u@u.t", "TESTPASSWORD1", "S1", "CS1"), "S1")
                            .orElse(null);
                    assertNull(resp);
                });
        val lockedUser = userInfoStore.getByEmail("u@u.t").orElse(null);
        assertNotNull(lockedUser);
        assertEquals(AuthState.LOCKED, lockedUser.getAuthState().getAuthState());

        //Test with correct password
        {
            ClientSession resp = authProvider.login(new PasswordAuthInfo("u@u.t", "TESTPASSWORD", "S1", "CS1"), "S1")
                    .orElse(null);
            assertNull(resp);
        }
        //Unlock
        userInfoStore.updateAuthState("UI", authState -> {
            authState.setAuthState(AuthState.ACTIVE);
            authState.setFailedAuthCount(0);
        });
        {
            ClientSession resp = authProvider.login(new PasswordAuthInfo("u@u.t", "TESTPASSWORD", "S1", "CS1"), "S2")
                    .orElse(null);
            assertNotNull(resp);
        }
    }

    @Test
    void testDeletedUser() {
        val af = new AuthenticationProviderFactory(authenticationConfig,
                                                   new ObjectMapper(),
                                                   () -> userInfoStore,
                                                   () -> sessionStore,
                                                   () -> passwordStore);

        val authProvider = af.create(authenticationConfig.getProvider());
        {
            ClientSession resp = authProvider.login(new PasswordAuthInfo("u@u.t", "TESTPASSWORD", "S1", "CS1"), "S2")
                    .orElse(null);
            assertNotNull(resp);
        }
        assertTrue(userInfoStore.deleteUser("UI"));
        {
            ClientSession resp = authProvider.login(new PasswordAuthInfo("u@u.t", "TESTPASSWORD", "S1", "CS1"), "S2")
                    .orElse(null);
            assertNull(resp);
        }
    }

    @Test
    void testInvalidUser() {
        val af = new AuthenticationProviderFactory(authenticationConfig,
                                                   new ObjectMapper(),
                                                   () -> userInfoStore,
                                                   () -> sessionStore,
                                                   () -> passwordStore);

        val authProvider = af.create(authenticationConfig.getProvider());
        {
            ClientSession resp = authProvider.login(new PasswordAuthInfo("u1@u.t", "TESTPASSWORD", "S1", "CS1"), "S2")
                    .orElse(null);
            assertNull(resp);
        }
    }
}