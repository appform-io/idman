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

import io.appform.idman.server.engine.results.*;

/**
 *
 */
public interface EngineEvalResultVisitor <T> {
    T visit(InvalidUser invalidUser);

    T visit(CredentialsExpired credentialsExpired);

    T visit(RedirectToParam redirectToParam);

    T visit(ViewOpSuccess renderView);

    T visit(InvalidService invalidService);

    T visit(ServiceOpSuccess serviceOpSuccess);

    T visit(GeneralOpSuccess generalOpSuccess);

    T visit(UserOpSuccess userOpSuccess);

    T visit(UserOpFailure userOpFailure);

    T visit(GeneralOpFailure generalOpFailure);

    T visit(RoleOpFailure roleOpFailure);

    T visit(RoleOpSuccess roleOpSuccess);
}
