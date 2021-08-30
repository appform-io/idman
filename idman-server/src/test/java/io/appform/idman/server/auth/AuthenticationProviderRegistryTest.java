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

package io.appform.idman.server.auth;

import io.appform.idman.model.AuthMode;
import io.appform.idman.server.auth.impl.PasswordAuthenticationProvider;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
                Map.of(AuthMode.PASSWORD, mock(PasswordAuthenticationProvider.class)));
        assertTrue(r.provider(AuthMode.PASSWORD).isPresent());
        assertFalse(r.provider(AuthMode.GOOGLE_AUTH).isPresent());
    }

}