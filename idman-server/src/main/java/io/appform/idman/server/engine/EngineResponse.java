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

package io.appform.idman.server.engine;

/**
 *
 */
public enum EngineResponse {

    INVALID_USER {
        @Override
        public <T> T accept(EngineResponseVisitor<T> visitor) {
            return visitor.invalidUser();
        }
    },
    PASSWORD_EXPIRED {
        @Override
        public <T> T accept(EngineResponseVisitor<T> visitor) {
            return visitor.passwordExpired();
        }
    };

    public interface EngineResponseVisitor<T> {

        T invalidUser();

        T passwordExpired();
    }

    public abstract <T> T accept(final EngineResponseVisitor<T> visitor);
}
