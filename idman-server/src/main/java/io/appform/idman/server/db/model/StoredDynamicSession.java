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

import io.appform.idman.model.TokenType;
import io.appform.idman.server.utils.Utils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.util.Date;

/**
 *
 */
@Entity
@Table(
        name = "dynamic_sessions",
        indexes = {
                @Index(name = "idx_ds_session", columnList = "session_id"),
                @Index(name = "idx_ds_user", columnList = "user_id"),
                @Index(name = "idx_ds_service_id", columnList = "service_id"),
                @Index(name = "idx_ds_service_client_session", columnList = "service_id, client_session_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "pk_id_ds_pid", columnNames = {"id", "partition_id"})
        }
)
@Data
@NoArgsConstructor
public class StoredDynamicSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "session_id", unique = true)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "client_session_id", nullable = false)
    private String clientSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private TokenType type;

    @Column
    private Date expiry;

    @Column(name = "partition_id")
    private int partitionId;

    @Column
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    public StoredDynamicSession(
            String sessionId,
            String userId,
            String serviceId,
            String clientSessionId,
            Date expiry) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.serviceId = serviceId;
        this.clientSessionId = clientSessionId;
        this.type = TokenType.DYNAMIC;
        this.expiry = expiry;
        this.partitionId = Utils.weekOfYear();
    }
}
