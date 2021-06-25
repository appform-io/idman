package io.appform.idman.client;

import io.appform.idman.model.*;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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
        public Optional<TokenInfo> accessToken(String serviceId, String tokenId) {
            return validateTokenImpl(serviceId, tokenId);
        }

        @Override
        public Optional<TokenInfo> validateTokenImpl(String serviceId, String token) {
            if (!status) {
                return Optional.empty();
            }
            invocations++;
            if (invocations == 1) {
                return Optional.of(new TokenInfo(null, null, 0L, "bearer", TEST_USER.getRole(), TEST_USER));
            }
            throw new IllegalAccessError("Should have been called only once");
        }

        @Override
        public boolean deleteToken(String userId, String serviceId, String jwt) {
            return false;
        }
    }

    @Test
    void validateInvalidParams() {
        assertFalse(new TestClient(false).validateToken(null, "T").isPresent());
        assertFalse(new TestClient(false).validateToken("S", null).isPresent());
    }

    @Test
    void validateFailure() {
        assertFalse(new TestClient(false).validateToken("S", "T").isPresent());
    }

    @Test
    void validateSuccess() {
        final TestClient client = new TestClient(true);

        assertTrue(client.validateToken("S", "T").isPresent());
        assertTrue(client.validateToken("S", "T").isPresent());
        assertTrue(client.validateToken("S", "T").isPresent());
        assertEquals(1, client.getInvocations());
    }

}