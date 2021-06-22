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

package io.appform.idman.server.db;

import io.appform.idman.server.db.model.ClientSession;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * For session management
 */
public interface SessionStoreForType {
    Optional<ClientSession> create(
            String sessionId,
            String userId,
            String serviceId,
            String clientSessionId,
            Date expiry);
    Optional<ClientSession> get(String sessionId);
    List<ClientSession> sessionsForUser(String userId);
    boolean delete(String sessionId);
}
