package io.appform.idman.server.auth;

import com.google.common.collect.ImmutableMap;
import io.appform.idman.model.AuthMode;
import io.appform.idman.server.auth.impl.PasswordAuthenticationProvider;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 *
 */
class AuthenticationProviderRegistryTest {

    @Test
    void testProvider() {
        val r = new AuthenticationProviderRegistry(
                ImmutableMap.of(AuthMode.PASSWORD, mock(PasswordAuthenticationProvider.class)));
        assertTrue(r.provider(AuthMode.PASSWORD).isPresent());
        assertFalse(r.provider(AuthMode.GOOGLE_AUTH).isPresent());
    }

}