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

import io.appform.idman.server.db.UserRoleStore;
import io.appform.idman.server.db.model.StoredUserRole;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class DBUserRoleStoreTest {

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(StoredUserRole.class)
            .build();

    private UserRoleStore roleStore;

    @BeforeEach
    void setup() {
        roleStore = new DBUserRoleStore(database.getSessionFactory());
    }

    @Test
    void testMapUnmap() {
        database.inTransaction(() -> roleStore.mapUserToRole("UI", "S1", "S1_TEST", "TEST"));
        {
            val fetched = database.inTransaction(() -> roleStore.getUserRoles("UI"));
            assertEquals(1, fetched.size());
            val role = fetched.get(0);
            assertEquals("UI", role.getUserId());
            assertEquals("S1", role.getServiceId());
            assertEquals("S1_TEST", role.getRoleId());
            assertFalse(role.isDeleted());
        }
        database.inTransaction(() -> roleStore.mapUserToRole("UI", "S1", "S1_TEST2", "TEST"));
        {
            val fetched = database.inTransaction(() -> roleStore.getUserRoles("UI"));
            assertEquals(1, fetched.size());
            val role = fetched.get(0);
            assertEquals("UI", role.getUserId());
            assertEquals("S1", role.getServiceId());
            assertEquals("S1_TEST2", role.getRoleId());
            assertFalse(role.isDeleted());
        }
        assertFalse(database.inTransaction(() -> roleStore.unmapUserFromRole("UI", "S2")));
        assertTrue(database.inTransaction(() -> roleStore.unmapUserFromRole("UI", "S1")));
        {
            val fetched = database.inTransaction(() -> roleStore.getUserRoles("UI"));
            assertTrue(fetched.isEmpty());
        }
//        database.inTransaction(() -> roleStore.getUserRoles("UI"));
    }

    @Test
    void testMultiUserForService() {
        IntStream.rangeClosed(1, 10)
                .forEach(i -> database.inTransaction(
                        () -> roleStore.mapUserToRole(
                                "U" + i, "S1", "S1_TEST", "TEST")));
        assertEquals(10, database.inTransaction(() -> roleStore.getServiceRoleMappings("S1")).size());
        assertTrue(IntStream.rangeClosed(1, 10)
                           .filter(i -> i % 2 == 0)
                           .allMatch(i -> database.inTransaction(() -> roleStore.unmapUserFromRole("U" + i, "S1"))));
        assertTrue(IntStream.rangeClosed(1, 10)
                           .filter(i -> i % 2 != 0)
                           .mapToObj(i -> "U" + i)
                           .collect(Collectors.toSet())
                           .containsAll(database.inTransaction(() -> roleStore.getServiceRoleMappings("S1"))
                                                .stream()
                                                .map(StoredUserRole::getUserId)
                                                .collect(Collectors.toSet())));
    }

    @Test
    void testMultiServiceForUser() {
        IntStream.rangeClosed(1, 10)
                .forEach(i -> database.inTransaction(
                        () -> roleStore.mapUserToRole(
                                "U1", "S" + i, "S" + i + "_TEST", "TEST")));
        assertEquals(10, database.inTransaction(() -> roleStore.getUserRoles("U1")).size());
        assertTrue(IntStream.rangeClosed(1, 10)
                           .mapToObj(i -> "S" + i + "_TEST")
                           .collect(Collectors.toSet())
                           .containsAll(
                                   IntStream.rangeClosed(1, 10)
                                           .mapToObj(i -> database.inTransaction(
                                                   () -> roleStore.getUserServiceRole("U1", "S" + i)))
                                           .filter(Optional::isPresent)
                                           .map(Optional::get)
                                           .map(StoredUserRole::getRoleId)
                                           .collect(Collectors.toSet())));
        assertTrue(IntStream.rangeClosed(1, 10)
                           .filter(i -> i % 2 == 0)
                           .allMatch(i -> database.inTransaction(() -> roleStore.unmapUserFromRole("U1", "S" + i))));
        assertTrue(IntStream.rangeClosed(1, 10)
                           .filter(i -> i % 2 != 0)
                           .mapToObj(i -> "U" + i)
                           .collect(Collectors.toSet())
                           .containsAll(database.inTransaction(() -> roleStore.getUserRoles("U1"))
                                                .stream()
                                                .map(StoredUserRole::getUserId)
                                                .collect(Collectors.toSet())));
    }
}