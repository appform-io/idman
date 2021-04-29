package io.appform.idman.server.db.impl;

import io.appform.idman.server.db.PasswordStore;
import io.appform.idman.server.db.model.StoredPassword;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class DBPasswordStoreTest {
    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(StoredPassword.class)
            .build();

    private PasswordStore passwordStore;

    @BeforeEach
    void setup() {
        passwordStore = new DBPasswordStore(database.getSessionFactory());
    }

    @Test
    void testSet() {
        //Test set
        database.inTransaction(() -> passwordStore.set("TU1", "password"));
        assertTrue(database.inTransaction(() -> passwordStore.match("TU1", "password")));
        assertFalse(database.inTransaction(() -> passwordStore.match("TU1", "password1")));

        //Test failed update
        assertFalse(database.inTransaction(() -> passwordStore.update("TU1", "password1", "password1")));

        //Test successful update
        assertTrue(database.inTransaction(() -> passwordStore.update("TU1", "password", "password1")));
        assertFalse(database.inTransaction(() -> passwordStore.match("TU1", "password")));
        assertTrue(database.inTransaction(() -> passwordStore.match("TU1", "password1")));

        //Test delete
        assertTrue(database.inTransaction(() -> passwordStore.delete("TU1")));
        assertFalse(database.inTransaction(() -> passwordStore.match("TU1", "password")));
        assertFalse(database.inTransaction(() -> passwordStore.match("TU1", "password1")));

        //Test undelete
        database.inTransaction(() -> passwordStore.set("TU1", "password"));
        assertTrue(database.inTransaction(() -> passwordStore.match("TU1", "password")));
        assertFalse(database.inTransaction(() -> passwordStore.match("TU1", "password1")));

        //Test invalid user
        assertFalse(database.inTransaction(() -> passwordStore.match("TU2", "password")));
        assertFalse(database.inTransaction(() -> passwordStore.update("TU2", "password", "password1")));
        assertFalse(database.inTransaction(() -> passwordStore.delete("TU2")));
    }

/*    @Test
    void testFailure() {
        passwordStore = spy(passwordStore);
        Mockito.doReturn(null).when(passwordStore).(anyString(), anyString());
        assertFalse(database.inTransaction(() -> passwordStore.set("TU1", "password")));

    }*/
}