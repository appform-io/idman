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

package io.appform.idman.server.utils;

import io.appform.idman.client.IdmanClientConfig;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.configs.JwtConfig;
import io.dropwizard.util.Duration;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.Collections;

/**
 *
 */
@UtilityClass
public class TestingUtils {
    public static AuthenticationConfig passwordauthConfig() {
        val authenticationConfig = new AuthenticationConfig();
        authenticationConfig.setDomain("testd");
        authenticationConfig.setServer("http://localhost:8080");
        authenticationConfig.setSessionDuration(Duration.days(7));

        val jwtConfig = new JwtConfig();
        jwtConfig.setIssuerId("testissuer");
        jwtConfig.setPrivateKey(
                "bYdNUUyCqx8IuGNqhFYS27WizZrfupAmJS8I4mfj2Cjox9Nc04Oews9tJEiDTrJfopzKdjygi8SgXeopSe/rPYqEKfrAUw/Dn6wMVhE56S7/5DKAvYusA2fQRqxOrOosO1lERnArw15tkAf/z5QUUUXnKZZTiczNEebjs2OG5s94PGxtQzxtYsZ1q2oXoq4lKPTosPpwkRxeh8LQCweDGR80xgoM1+yDAoYIeg==");
        authenticationConfig.setJwt(jwtConfig);
        return authenticationConfig;
    }

    public static IdmanClientConfig clientConfig() {
        val clientConfig = new IdmanClientConfig();
        clientConfig.setAuthEndpoint("http://localhost:8080");
        clientConfig.setResourcePrefix("/apis");
        clientConfig.setServiceId("testservice");
        clientConfig.setAllowedPaths(Collections.emptySet());
        return clientConfig;
    }
}
