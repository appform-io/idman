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

package io.appform.idman.authbundle;

import io.appform.idman.authcomponents.IdmanDynamicFeature;
import io.appform.idman.authcomponents.resource.IdmanAuthHandler;
import io.appform.idman.client.http.IdManHttpClientConfig;
import io.appform.idman.client.http.IdmanHttpClient;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Environment;
import lombok.val;

/**
 *
 */
public abstract class IdmanAuthBundle<T extends Configuration> implements ConfiguredBundle<T> {
    @Override
    public void run(T configuration, Environment environment) throws Exception {
        val config = clientConfig(configuration);
        val client = new IdmanHttpClient(config, environment.getObjectMapper());
        environment.jersey().register(new IdmanDynamicFeature(environment, config, client));
        environment.jersey().register(new IdmanAuthHandler(client, config));
    }

    public abstract IdManHttpClientConfig clientConfig(T config);
}
