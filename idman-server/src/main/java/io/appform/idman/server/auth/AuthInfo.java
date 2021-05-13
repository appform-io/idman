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
import lombok.Data;

/**
 * Base class for auth info. There would be one sub type for each auth mode
 */
@Data
public abstract class AuthInfo {
    private final AuthMode mode;
    private final String serviceId;
    private final String clientSessionId;

    protected AuthInfo(AuthMode mode, String serviceId, String clientSessionId) {
        this.mode = mode;
        this.serviceId = serviceId;
        this.clientSessionId = clientSessionId;
    }

    public abstract <T> T accept(final AuthInfoVisitor<T> visitor);
}
