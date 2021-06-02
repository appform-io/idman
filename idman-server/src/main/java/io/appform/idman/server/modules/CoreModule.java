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

package io.appform.idman.server.modules;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.appform.idman.client.IdManClient;
import io.appform.idman.client.IdmanClientConfig;
import io.appform.idman.model.AuthMode;
import io.appform.idman.server.AppConfig;
import io.appform.idman.server.auth.AuthenticationProvider;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.configs.AuthenticationProviderConfig;
import io.appform.idman.server.auth.impl.PasswordAuthenticationProvider;
import io.appform.idman.server.db.*;
import io.appform.idman.server.db.impl.*;
import io.appform.idman.server.localauth.LocalIdmanAuthClient;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import lombok.val;
import org.hibernate.SessionFactory;

import java.util.Map;

/**
 * Create Data access objects
 */
public class CoreModule extends AbstractModule {

    private final HibernateBundle<AppConfig> hibernate;

    public CoreModule(HibernateBundle<AppConfig> hibernate) {

        this.hibernate = hibernate;
    }

    @Override
    protected void configure() {
        bind(ServiceStore.class).to(DBServiceStore.class);
        bind(RoleStore.class).to(DBRoleStore.class);
        bind(UserRoleStore.class).to(DBUserRoleStore.class);
        bind(PasswordStore.class).to(DBPasswordStore.class);
        bind(SessionStore.class).to(DBSessionStore.class);
        bind(UserInfoStore.class).to(DBUserInfoStore.class);
    }

    @Provides
    @Singleton
    public SessionFactory sessionFactory() {
        return hibernate.getSessionFactory();
    }

    @Provides
    @Singleton
    public AuthenticationConfig authenticationConfig(AppConfig appConfig) {
        return appConfig.getAuthenticationCore();
    }

    @Provides
    @Singleton
    public AuthenticationProviderConfig gauthConfig(AuthenticationConfig authenticationConfig) {
        return authenticationConfig.getProvider();
    }

    @Provides
    @Singleton
    public IdmanClientConfig idmanAuthenticationConfig(AppConfig appConfig) {
        val idmanConf = new IdmanClientConfig();
        idmanConf.setServiceId("IDMAN");
        idmanConf.setAllowedPaths(ImmutableSet.of(
                "/auth/login",
                "/js",
                "/css",
                "/apis/idman",
                "/apis/auth",
                "ui/auth/login",
                "ui/setup",
                "/setup"));
        idmanConf.setResourcePrefix("/apis");
        idmanConf.setAuthEndpoint(appConfig.getAuthenticationCore().getServer());
        return idmanConf;
    }

    @Provides
    @Singleton
    public Map<AuthMode, AuthenticationProvider> registry(
            PasswordAuthenticationProvider credentialAuthenticationProvider) {
        return ImmutableMap.of(AuthMode.PASSWORD, credentialAuthenticationProvider);
    }

    @Provides
    @Singleton
    public IdManClient idManClient(
            SessionStore sessionStore,
            UserInfoStore userInfoStore,
            ServiceStore serviceStore,
            UserRoleStore roleStore,
            AuthenticationConfig authConfig) {

        return new UnitOfWorkAwareProxyFactory(hibernate)
                .create(LocalIdmanAuthClient.class,
                        new Class[]{
                                SessionStore.class,
                                UserInfoStore.class,
                                ServiceStore.class,
                                UserRoleStore.class,
                                AuthenticationConfig.class
                        },
                        new Object[]{
                                sessionStore,
                                userInfoStore,
                                serviceStore,
                                roleStore,
                                authConfig
                        });
    }
}
