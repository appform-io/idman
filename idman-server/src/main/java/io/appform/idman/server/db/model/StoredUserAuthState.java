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

import io.appform.idman.model.AuthMode;
import io.appform.idman.server.db.AuthState;
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
@Table(name = "user_auth_state")
@Data
@NoArgsConstructor
public class StoredUserAuthState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_mode", length = 45)
    private AuthMode authMode;

    @Column(name = "auth_state")
    @Enumerated(EnumType.STRING)
    private AuthState authState;

    @Column(name = "failed_auth_count")
    private int failedAuthCount;

    @OneToOne(mappedBy = "authState")
    private StoredUser user;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    public StoredUserAuthState(
            AuthMode authMode,
            AuthState authState,
            int failedAuthCount,
            StoredUser user) {
        this.authMode = authMode;
        this.authState = authState;
        this.failedAuthCount = failedAuthCount;
        this.user = user;
    }
}
