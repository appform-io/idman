package io.appform.idman.client;

import io.appform.idman.model.AuthMode;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.User;
import io.appform.idman.model.UserType;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
class IdManClientTest {
    private static final IdmanUser TEST_USER = new IdmanUser("S1",
                                                             "S",
                                                             new User("U1", "U", UserType.HUMAN, AuthMode.PASSWORD),
                                                             "S_ADMIN");

    private static class TestClient extends IdManClient {
        private final boolean status;
        @Getter
        private int invocations = 0;

        private TestClient(boolean status) {
            this.status = status;
        }

        @Override
        protected IdmanUser validateImpl(String token, String serviceId) {
            if (!status) {
                return null;
            }
            invocations++;
            if (invocations == 1) {
                return TEST_USER;
            }
            throw new IllegalAccessError("Should have been called only once");
        }
    }

    @Test
    void validateInvalidParams() {
        assertFalse(new TestClient(false).validate("T", null).isPresent());
        assertFalse(new TestClient(false).validate(null, "S").isPresent());
    }

    @Test
    void validateFailure() {
        assertFalse(new TestClient(false).validate("T", "S").isPresent());
    }

    @Test
    void validateSuccess() {
        final TestClient client = new TestClient(true);

        assertTrue(client.validate("T", "S").isPresent());
        assertTrue(client.validate("T", "S").isPresent());
        assertTrue(client.validate("T", "S").isPresent());
        assertEquals(1, client.getInvocations());
    }

}