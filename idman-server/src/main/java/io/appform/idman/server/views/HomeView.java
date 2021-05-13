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

package io.appform.idman.server.views;

import io.appform.idman.model.IdmanUser;
import io.appform.idman.server.db.model.StoredService;
import io.appform.idman.server.db.model.StoredUser;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import java.util.List;

/**
 * Renders the homepage
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HomeView extends TemplateView {
    List<StoredService> services;
    List<StoredUser> users;
    IdmanUser sessionUser;

    public HomeView(
            List<StoredService> services,
            List<StoredUser> users,
            IdmanUser sessionUser) {
        super("templates/home.hbs");
        this.services = services;
        this.users = users;
        this.sessionUser = sessionUser;
    }
}
