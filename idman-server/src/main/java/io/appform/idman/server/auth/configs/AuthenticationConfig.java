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

import io.dropwizard.util.Duration;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 *
 */
@Data
public class AuthenticationConfig {

    @NotNull
    @Valid
    private JwtConfig jwt = new JwtConfig();

    @NotNull
    @Valid
    private AuthenticationProviderConfig provider = new CredentialAuthenticationProviderConfig();

    private String domain;

    @NotNull
    @NotEmpty
    @URL
    private String server;

    @NotNull
    private Duration sessionDuration = Duration.days(30);

    @NotNull
    private Duration maxDynamicTokenRefreshInterval = Duration.minutes(15);
}
