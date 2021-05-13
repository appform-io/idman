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

package io.appform.idman.model;

import lombok.Value;

import java.util.Date;

/**
 * A service being provided by the organization
 */
@Value
public class Service {

    /**
     * Unique ID of the service
     */
    String serviceId;

    /**
     * A Human readable name for the service
     */
    String name;

    /**
     * Service description
     */
    String description;

    /**
     * Has this been deleted
     */
    boolean deleted;

    /**
     * Date when service was created
     */
    Date created;

    /**
     * Date when the service was last updated
     */
    Date updated;
}
