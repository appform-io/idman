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

import io.appform.idman.server.db.SessionStoreForType;
import io.appform.idman.model.TokenType;
import io.appform.idman.server.db.model.StoredStaticSession;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class DBStaticSessionStoreTest {
    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(StoredStaticSession.class)
            .build();

    private SessionStoreForType store;

    @BeforeEach
    void setup() {
        this.store = new DBStaticSessionStore(database.getSessionFactory());
    }

    @Test
    void testSessionCrud() {
        val session = database.inTransaction(
                () -> store.create("SS1", "U1", "S1", "CS1", null))
                .orElse(null);
        assertNotNull(session);
        assertNull(database.inTransaction(() -> store.get("SS1x")).orElse(null));
        {
            val fetched = database.inTransaction(() -> store.get("SS1")).orElse(null);
            assertNotNull(fetched);
            assertEquals("SS1", fetched.getSessionId());
            assertEquals("U1", fetched.getUserId());
            assertEquals("S1", fetched.getServiceId());
            assertEquals("CS1", fetched.getClientSessionId());
            assertEquals(TokenType.STATIC, fetched.getType());
            assertNull(fetched.getExpiry());
            assertFalse(fetched.isDeleted());
        }
        {
            val list = database.inTransaction(() -> store.sessionsForUser("U1"));
            assertFalse(list.isEmpty());
            assertEquals(1, list.size());
            val fetched = list.get(0);
            assertEquals("SS1", fetched.getSessionId());
            assertEquals("U1", fetched.getUserId());
            assertEquals("S1", fetched.getServiceId());
            assertEquals("CS1", fetched.getClientSessionId());
            assertEquals(TokenType.STATIC, fetched.getType());
            assertNull(fetched.getExpiry());
            assertFalse(fetched.isDeleted());
        }
        try {
            database.inTransaction(
                    () -> store.create("SS1", "U1", "S1", "CS1", null));
            fail("Should have thrown exception");
        }
        catch (IllegalArgumentException e) {
            assertEquals("Session exists", e.getMessage());
        }
        assertFalse(database.inTransaction((() -> store.delete("SS1x"))));
        assertTrue(database.inTransaction((() -> store.delete("SS1"))));
        assertNull(database.inTransaction(() -> store.get("SS1")).orElse(null));
    }

    @Test
    void testExpiry() {
        try {
            val session = database.inTransaction(
                    () -> store.create("SS1", "U1", "S1", "CS1", new Date()))
                    .orElse(null);
        } catch (Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }
}