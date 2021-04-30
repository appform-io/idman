package io.appform.idman.server.db.impl;

import io.appform.idman.server.db.RoleStore;
import io.appform.idman.server.db.model.StoredRole;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class DBRoleStoreTest {

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(StoredRole.class)
            .build();

    private RoleStore roleStore;

    @BeforeEach
    void setup() {
        roleStore = new DBRoleStore(database.getSessionFactory());
    }

    @Test
    void testCreateUndelete() {
        val sId = "S1";

        val role = database.inTransaction(() -> roleStore.create(sId, "Test Role", "Default desc"))
                .orElse(null);
        assertNotNull(role);
        assertEquals("S1_TEST_ROLE", role.getRoleId());
        assertEquals("Test Role", role.getDisplayName());
        assertEquals("Default desc", role.getDescription());
        assertFalse(role.isDeleted());
        assertEquals(sId, role.getServiceId());

        {
            val fetched = roleStore.get(sId, role.getRoleId()).orElse(null);
            assertNotNull(fetched);
            assertEquals(role, fetched);
        }
        {
            assertFalse(roleStore.delete(sId+ "x", role.getRoleId()));
            assertTrue(roleStore.delete(sId, role.getRoleId()));
            val fetched = roleStore.get(sId, role.getRoleId()).orElse(null);
            assertNotNull(fetched);
            assertTrue(fetched.isDeleted());
        }
        {
            val fetched = database.inTransaction(() -> roleStore.create(sId, "Test Role", "Default desc"))
                    .orElse(null);
            assertNotNull(fetched);
            assertFalse(fetched.isDeleted());
            assertEquals(role.getId(), fetched.getId());
        }
    }

    @Test
    void testUpdate() {
        val sId = "S1";

        val role = database.inTransaction(() -> roleStore.create(sId, "Test Role", "Default desc"))
                .orElse(null);
        assertNotNull(role);
        assertEquals("S1_TEST_ROLE", role.getRoleId());

        {
            val updated = database.inTransaction(() -> roleStore.update(sId, role.getRoleId(), "New desc"))
                    .orElse(null);
            assertNotNull(updated);
            assertEquals("S1_TEST_ROLE", updated.getRoleId());
            assertEquals("New desc", updated.getDescription());
        }
        {
            assertNull(database.inTransaction(() -> roleStore.update(sId + "x", role.getRoleId(), "New desc"))
                               .orElse(null));
        }
    }

    @Test
    void testMulti() {
        val sId = "S1";
        val roles = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> database.inTransaction(() -> roleStore.create(sId, "Role_" + i, "Desc").orElse(null)))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(StoredRole::getRoleId))
                .collect(Collectors.toList());
        assertEquals(10, roles.size());

        {
            val fetched = database.inTransaction(() -> roleStore.get(roles.stream()
                                                                             .map(StoredRole::getRoleId)
                                                                             .collect(Collectors.toList())));
            assertEquals(roles, fetched);
        }
        {
            assertTrue(IntStream.rangeClosed(1, 10)
                               .filter(i -> i % 2 == 0)
                               .allMatch(i -> database.inTransaction(() -> roleStore.delete(sId, "S1_ROLE_" + i))));
            assertEquals(5, database.inTransaction(() -> roleStore.list(sId, false)).size());
            assertEquals(10, database.inTransaction(() -> roleStore.list(sId, true)).size());
        }
    }
}