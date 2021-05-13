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

import io.appform.idman.model.AuthMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import java.net.Proxy;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GoogleAuthenticationProviderConfig extends AuthenticationProviderConfig {
    @NotEmpty
    private String clientId;

    @NotEmpty
    private String clientSecret;

    private String loginDomain;

    private Proxy.Type proxyType;

    private String proxyHost;

    private int proxyPort;

    public GoogleAuthenticationProviderConfig() {
        super(AuthMode.GOOGLE_AUTH);
    }

    @Override
    public <T> T accept(AuthenticationProviderConfigVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
