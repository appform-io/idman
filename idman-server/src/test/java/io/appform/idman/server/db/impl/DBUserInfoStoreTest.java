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

package io.appform.idman.server.db.impl;

import io.appform.idman.model.AuthMode;
import io.appform.idman.model.UserType;
import io.appform.idman.server.db.AuthState;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.db.model.StoredUserAuthState;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.val;
import lombok.var;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class DBUserInfoStoreTest {

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(StoredUser.class)
            .addEntityClass(StoredUserAuthState.class)
            .build();

    private UserInfoStore userInfoStore;

    @BeforeEach
    public void setup() {
        userInfoStore = new DBUserInfoStore(database.getSessionFactory());
    }

    @Test
    void testUserCreateUndelete() {
        val createdUser = database.inTransaction(() -> userInfoStore.create("TU1",
                                                                            "test@test.com",
                                                                            "Test",
                                                                            UserType.HUMAN,
                                                                            AuthMode.PASSWORD))
                .orElse(null);
        assertNotNull(createdUser);
        assertEquals("TU1", createdUser.getUserId());
        assertEquals("test@test.com", createdUser.getEmail());
        assertEquals("Test", createdUser.getName());
        assertEquals(UserType.HUMAN, createdUser.getUserType());
        assertFalse(createdUser.isDeleted());

        val authState = createdUser.getAuthState();
        assertEquals(AuthMode.PASSWORD, authState.getAuthMode());
        assertEquals(AuthState.EXPIRED, authState.getAuthState());
        assertEquals(0, authState.getFailedAuthCount());
        assertEquals(createdUser, authState.getUser());

        assertTrue(database.inTransaction(() -> userInfoStore.deleteUser("TU1")));
        var deletedUser = database.inTransaction(() -> userInfoStore.get("TU1")).orElse(null);
        assertNotNull(deletedUser);
        assertTrue(deletedUser.isDeleted());

        var updatedUser = database.inTransaction(() -> userInfoStore.create("TU1",
                                                                            "test1@test.com",
                                                                            "Test2",
                                                                            UserType.HUMAN,
                                                                            AuthMode.PASSWORD))
                .orElse(null);
        assertNotNull(updatedUser);
        assertNotEquals("test1@test.com", updatedUser.getEmail());
        assertEquals("test@test.com", updatedUser.getEmail()); //Email will not be updated
        assertEquals("Test2", updatedUser.getName());
        assertFalse(createdUser.isDeleted());

        var user = database.inTransaction(() -> userInfoStore.get("TU1")).orElse(null);
        assertNotNull(user);
        assertEquals(user, createdUser);

        assertFalse(database.inTransaction(() -> userInfoStore.deleteUser("TU2")));

        user = database.inTransaction(() -> userInfoStore.getByEmail("test@test.com")).orElse(null);
        assertEquals(updatedUser, user);
    }

    @Test
    void testUpdate() {
        val createdUser = database.inTransaction(() -> userInfoStore.create("TU1",
                                                                            "test@test.com",
                                                                            "Test",
                                                                            UserType.HUMAN,
                                                                            AuthMode.PASSWORD))
                .orElse(null);
        assertNotNull(createdUser);
        assertEquals("TU1", createdUser.getUserId());
        assertEquals("test@test.com", createdUser.getEmail());
        assertEquals("Test", createdUser.getName());
        assertEquals(UserType.HUMAN, createdUser.getUserType());
        assertFalse(createdUser.isDeleted());

        var updatedUser = database.inTransaction(() -> userInfoStore.updateName("TU1", "Test 3")
                .orElse(null));
        assertNotNull(updatedUser);
        assertEquals("Test 3", updatedUser.getName());

        updatedUser = database.inTransaction(() -> userInfoStore.updateAuthState(
                "TU1", state -> state.setAuthState(AuthState.ACTIVE))
                .orElse(null));
        assertNotNull(updatedUser);
        assertEquals(AuthState.ACTIVE, updatedUser.getAuthState().getAuthState());
    }

    @Test
    void multiTest() {
        val users = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> database.inTransaction(() -> userInfoStore.create("TU" + i,
                                                                                 "test" + i + "@test.com",
                                                                                 "Test " + i,
                                                                                 UserType.HUMAN,
                                                                                 AuthMode.PASSWORD)))
                .filter(Objects::nonNull)
                .map(Optional::get)
                .collect(Collectors.toList());
        assertEquals(10, users.size());

        val userIds = users.stream()
                .map(StoredUser::getUserId)
                .sorted()
                .collect(Collectors.toList());
        var selectedUsers = database.inTransaction(() -> userInfoStore.get(userIds))
                .stream()
                .map(StoredUser::getUserId)
                .collect(Collectors.toList());
        assertEquals(userIds, selectedUsers);


        selectedUsers = database.inTransaction(() -> userInfoStore.list(true))
                .stream()
                .map(StoredUser::getUserId)
                .collect(Collectors.toList());
        assertEquals(userIds, selectedUsers);

        selectedUsers = database.inTransaction(() -> userInfoStore.list(false))
                .stream()
                .map(StoredUser::getUserId)
                .collect(Collectors.toList());
        assertEquals(userIds, selectedUsers);

        assertTrue(IntStream.rangeClosed(1,10)
                .filter(i -> i % 2 == 0)
                .allMatch(i -> database.inTransaction(() -> userInfoStore.deleteUser("TU" + i))));

        selectedUsers = database.inTransaction(() -> userInfoStore.list(true))
                .stream()
                .map(StoredUser::getUserId)
                .collect(Collectors.toList());
        assertEquals(userIds, selectedUsers);

        var deletedUsers = database.inTransaction(() -> userInfoStore.list(false))
                .stream()
                .map(StoredUser::getUserId)
                .collect(Collectors.toList());
        assertEquals(5, deletedUsers.size());
        assertTrue(IntStream.rangeClosed(1,10)
                .mapToObj(i -> "TU" + i)
                .collect(Collectors.toSet())
                .containsAll(deletedUsers));
    }

    @Test
    void testPasswordActive() {
        val createdUser = database.inTransaction(() -> userInfoStore.create("TU1",
                                                                            "test@test.com",
                                                                            "Test",
                                                                            UserType.HUMAN,
                                                                            AuthMode.PASSWORD,
                                                                            false))
                .orElse(null);
        assertNotNull(createdUser);
        assertEquals("TU1", createdUser.getUserId());
        assertEquals("test@test.com", createdUser.getEmail());
        assertEquals("Test", createdUser.getName());
        assertEquals(UserType.HUMAN, createdUser.getUserType());
        assertFalse(createdUser.isDeleted());

        val authState = createdUser.getAuthState();
        assertEquals(AuthMode.PASSWORD, authState.getAuthMode());
        assertEquals(AuthState.ACTIVE, authState.getAuthState());
        assertEquals(0, authState.getFailedAuthCount());
        assertEquals(createdUser, authState.getUser());
    }
}