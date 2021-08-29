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

import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.db.model.StoredService;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.util.Strings;
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
class DBServiceStoreTest {
    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(StoredService.class)
            .build();

    private ServiceStore serviceStore;

    @BeforeEach
    void setup() {
        serviceStore = new DBServiceStore(database.getSessionFactory());
    }

    @Test
    void testCreateUndelete() {
        var service = database.inTransaction(() -> serviceStore.create("TestS", "Test service", "https://idman.test"))
                .orElse(null);

        assertNotNull(service);
        val serviceId = service.getServiceId();
        assertEquals("TESTS", service.getServiceId());
        assertEquals("TestS", service.getName());
        assertEquals("Test service", service.getDescription());
        assertEquals("https://idman.test", service.getCallbackUrl());
        assertFalse(Strings.isNullOrEmpty(service.getSecret()));
        assertFalse(service.isDeleted());

        var fetchedService = serviceStore.get(serviceId).orElse(null);
        assertNotNull(fetchedService);
        assertEquals(service, fetchedService);

        assertTrue(serviceStore.delete(serviceId));
        assertFalse(serviceStore.delete("AA"));
        fetchedService = serviceStore.get(serviceId).orElse(null);
        assertNotNull(fetchedService);
        assertTrue(fetchedService.isDeleted());

        val updatedS = database.inTransaction(() -> serviceStore.create("TestS", "Test service", "https://idman.test"))
                .orElse(null);
        assertNotNull(updatedS);
        assertEquals(service.getServiceId(), updatedS.getServiceId());
        assertEquals(service.getName(), fetchedService.getName());
        assertFalse(fetchedService.isDeleted());
    }

    @Test
    void testUodates() {
        var service = database.inTransaction(() -> serviceStore.create("TestS", "Test service", "https://idman.test"))
                .orElse(null);

        assertNotNull(service);
        assertEquals("Test service", service.getDescription());
        assertEquals("https://idman.test", service.getCallbackUrl());
        assertFalse(Strings.isNullOrEmpty(service.getSecret()));

        val serviceId = service.getServiceId();

        {
            val fetchedService
                    = database.inTransaction(() -> serviceStore.updateDescription(serviceId, "Abc"))
                    .orElse(null);
            assertNotNull(fetchedService);
            assertEquals("Abc", service.getDescription());
            assertFalse(database.inTransaction(() -> serviceStore.updateDescription("XX", "XX"))
                                .isPresent());
        }

        {
            val fetchedService
                    = database.inTransaction(() -> serviceStore.updateCallbackUrl(serviceId, "localhost"))
                    .orElse(null);
            assertNotNull(fetchedService);
            assertEquals("localhost", service.getCallbackUrl());
            assertFalse(database.inTransaction(() -> serviceStore.updateCallbackUrl("XX", "XX"))
                                .isPresent());
        }
        {
            val oldSecret = service.getSecret();
            val fetchedService
                    = database.inTransaction(() -> serviceStore.updateSecret(serviceId))
                    .orElse(null);
            assertNotNull(fetchedService);
            assertFalse(Strings.isNullOrEmpty(fetchedService.getSecret()));
            assertNotEquals(oldSecret, service.getSecret());
            assertFalse(database.inTransaction(() -> serviceStore.updateSecret("XX"))
                                .isPresent());

        }
    }

    @Test
    void testMulti() {
        val services = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> serviceStore.create("S" + i, "Srv " + i, "localhost:" + i))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        assertEquals(10, services.size());
        var ids = services.stream().map(StoredService::getServiceId).collect(Collectors.toSet());

        {
            val fetched = database.inTransaction(() -> serviceStore.get(ids))
                    .stream()
                    .map(StoredService::getServiceId)
                    .collect(Collectors.toSet());
            assertTrue(ids.containsAll(fetched));
        }
        {
            val fetched = database.inTransaction(() -> serviceStore.list(false));
            assertTrue(ids.containsAll(fetched.stream()
                                               .map(StoredService::getServiceId)
                                               .sorted()
                                               .collect(Collectors.toList())));
        }
        {
            IntStream.rangeClosed(1, 10)
                    .filter(i -> i % 2 == 0)
                    .forEach(i -> database.inTransaction(() -> serviceStore.delete("S" + i)));
            val fetched = database.inTransaction(() -> serviceStore.list(false));
            assertEquals(5, fetched.size());
            assertEquals(10, database.inTransaction(() -> serviceStore.list(true)).size());
            val expected = IntStream.rangeClosed(1, 10)
                    .filter(i -> i % 2 != 0)
                    .mapToObj(i -> "S" + i)
                    .collect(Collectors.toSet());
            System.out.println(expected);
            assertTrue(fetched.stream()
                               .map(StoredService::getServiceId)
                               .collect(Collectors.toSet())
                               .containsAll(expected));
        }
    }
}