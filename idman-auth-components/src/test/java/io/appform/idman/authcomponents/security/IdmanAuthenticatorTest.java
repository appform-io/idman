package io.appform.idman.authcomponents.security;

import io.appform.idman.client.IdManClient;
import io.appform.idman.client.IdmanClientConfig;
import io.appform.idman.model.AuthMode;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.User;
import io.appform.idman.model.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 *
 */
class IdmanAuthenticatorTest {
    private static final IdmanUser TEST_USER = new IdmanUser("S1",
                                                             "S",
                                                             new User("U1", "U", UserType.HUMAN, AuthMode.PASSWORD),
                                                             "S_ADMIN");

    private IdManClient client = mock(IdManClient.class);
    final IdmanClientConfig config = new IdmanClientConfig();
    private IdmanAuthenticator authenticator = new IdmanAuthenticator(config, client);

    {
        config.setServiceId("S");
    }

    @AfterEach
    void destroy() {
        reset(client);
    }

    @Test
    void testInvToken() {
        doReturn(Optional.empty()).when(client).validate(anyString(), anyString());
        assertFalse(authenticator.authenticate("T").isPresent());
    }

    @Test
    void testAuthSuccess() {
        doReturn(Optional.of(TEST_USER)).when(client).validate(anyString(), anyString());
        assertTrue(authenticator.authenticate("T").isPresent());
    }
}