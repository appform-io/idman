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

package io.appform.idman.server.auth.configs;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.idman.model.AuthMode;
import lombok.Data;

/**
 * Config class for {@link io.appform.idman.server.auth.AuthenticationProvider}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "GOOGLE_AUTH", value = GoogleAuthenticationProviderConfig.class),
        @JsonSubTypes.Type(name = "CREDENTIAL", value = CredentialAuthenticationProviderConfig.class)
})
@Data
public abstract class AuthenticationProviderConfig {
    private final AuthMode type;
    private boolean enabled;

    protected AuthenticationProviderConfig(AuthMode type) {
        this.type = type;
    }

    public abstract <T> T accept(AuthenticationProviderConfigVisitor<T> visitor);
}
