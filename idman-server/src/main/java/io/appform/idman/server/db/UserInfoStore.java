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

import io.appform.idman.model.AuthMode;
import io.appform.idman.model.UserType;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.db.model.StoredUserAuthState;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 *
 */
public interface UserInfoStore {

    default Optional<StoredUser> create(String userId, String email, String name, UserType userType, AuthMode authMode) {
        return create(userId, email, name, userType, authMode, true);
    }

    Optional<StoredUser> create(String userId, String email, String name, UserType userType, AuthMode authMode, boolean expire);
    Optional<StoredUser> get(String userId);
    Optional<StoredUser> getByEmail(String email);
    Optional<StoredUser> updateName(String userId, String name);
    Optional<StoredUser> updateAuthState(String userId, Consumer<StoredUserAuthState> handler);
    boolean deleteUser(String userId);
    List<StoredUser> list(boolean includeDeleted);
    List<StoredUser> get(Collection<String> userIds);
}
