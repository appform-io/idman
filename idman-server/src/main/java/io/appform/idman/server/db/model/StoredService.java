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

package io.appform.idman.server.db.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.SQLDelete;

import javax.persistence.*;
import java.util.Date;

/**
 *
 */
@Entity
@Table(name = "services",
        indexes = {
                @Index(name = "idx_service", columnList = "service_id")
        })
@SQLDelete(sql = "UPDATE services SET deleted = '1' WHERE id = ?")
@Data
@NoArgsConstructor
public class StoredService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "service_id", unique = true, nullable = false, length = 45)
    private String serviceId;

    @Column(nullable = false, length = 45)
    private String name;

    @Column
    private String description;

    @Column(name = "callback_url")
    private String callbackUrl;

    @Column(name = "secret")
    private String secret;

    @Column
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    public StoredService(String serviceId, String name, String description, String callbackUrl, String secret) {
        this.serviceId = serviceId;
        this.name = name;
        this.description = description;
        this.callbackUrl = callbackUrl;
        this.secret = secret;
    }
}
