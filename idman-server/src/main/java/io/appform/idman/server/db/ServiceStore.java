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

import io.appform.idman.server.db.model.StoredService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface ServiceStore {
    Optional<StoredService> create(String name, String description, String callbackUrl);
    Optional<StoredService> get(String serviceId);
    Optional<StoredService> updateDescription(String serviceId, String description);
    Optional<StoredService> updateCallbackUrl(String serviceId, String callbackUrl);
    Optional<StoredService> updateSecret(String serviceId);
    boolean delete(String serviceId);

    List<StoredService> get(Collection<String> serviceIds);

    List<StoredService> list(boolean includeDeleted);
}
