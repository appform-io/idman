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

package io.appform.idman.server.handlebars;

import com.github.jknack.handlebars.Options;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.server.auth.IdmanRoles;
import io.appform.idman.server.db.model.StoredUser;

import java.io.IOException;

/**
 *
 */

public class CustomHelpers {

    public CharSequence admin(final IdmanUser user, final Options options) throws IOException {
        return null != user && null != user.getRole() && user.getRole().equals(IdmanRoles.ADMIN)
                ? options.fn()
               : options.inverse();
    }

    public CharSequence adminOrSelf(final IdmanUser idmanUser, final StoredUser user, final Options options) throws IOException {
        return ((null != idmanUser && null != idmanUser.getRole() && idmanUser.getRole().equals(IdmanRoles.ADMIN))
                || (null != user && null != idmanUser && user.getUserId().equals(idmanUser.getUser().getId())))
                ? options.fn()
               : options.inverse();
    }
}
