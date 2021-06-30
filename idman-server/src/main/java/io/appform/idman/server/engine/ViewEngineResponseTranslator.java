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

import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.engine.results.*;
import io.appform.idman.server.utils.Utils;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 *
 */
public class ViewEngineResponseTranslator implements EngineEvalResultVisitor<Response> {

    private final AuthenticationConfig config;

    @Inject
    public ViewEngineResponseTranslator(AuthenticationConfig config) {
        this.config = config;
    }

    public final Response translate(final EngineEvalResult result) {
        return result.accept(this);
    }

    @Override
    public Response visit(InvalidUser invalidUser) {
        return redirectToLogin();
    }

    @Override
    public Response visit(CredentialsExpired credentialsExpired) {
        return redirectToPasswordChangePage(credentialsExpired.getUserId());
    }

    @Override
    public Response visit(RedirectToParam redirectToParam) {
        return redirectToPage(redirectToParam.getRedirect());
    }

    @Override
    public Response visit(ViewOpSuccess renderView) {
        return Response.ok(renderView.getView()).build();
    }

    @Override
    public Response visit(InvalidService invalidService) {
        return redirectToHome();
    }

    @Override
    public Response visit(ServiceOpSuccess serviceOpSuccess) {
        return redirectToServicePage(serviceOpSuccess.getServiceId());
    }

    @Override
    public Response visit(GeneralOpSuccess generalOpSuccess) {
        return redirectToHome();
    }

    @Override
    public Response visit(GeneralOpFailure generalOpFailure) {
        return redirectToHome();
    }

    @Override
    public Response visit(UserOpSuccess userOpSuccess) {
        return redirectToUserPage(userOpSuccess.getUserId());
    }

    @Override
    public Response visit(UserOpFailure userOpFailure) {
        return redirectToUserPage(userOpFailure.getUserId());
    }


    @Override
    public Response visit(RoleOpFailure roleOpFailure) {
        return redirectToServicePage(roleOpFailure.getServiceId());
    }

    @Override
    public Response visit(RoleOpSuccess roleOpSuccess) {
        return redirectToServicePage(roleOpSuccess.getServiceId());
    }

    @Override
    public Response visit(TokenOpSuccess tokenOpSuccess) {
        return redirectToPage("/tokens/" + tokenOpSuccess.getServiceId()
                                      + "/" + tokenOpSuccess.getUserId()
                                      + "/" + tokenOpSuccess.getSessionId());
    }

    private Response redirectToLogin() {
        return redirectToPage("/auth/login");
    }

    private Response redirectToHome() {
        return redirectToPage("/");
    }

    private Response redirectToServicePage(final String serviceId) {
        return redirectToPage("/services/" + serviceId);
    }

    private Response redirectToUserPage(final String userId) {
        return redirectToPage("/users/" + userId);
    }

    private Response redirectToPasswordChangePage(final String userId) {
        return redirectToPage("/users/" + userId + "/update/password");
    }

    private Response redirectToPage(final String path) {
        return Response.seeOther(URI.create(Utils.createUri(config.getServer(), path))).build();
    }

}
