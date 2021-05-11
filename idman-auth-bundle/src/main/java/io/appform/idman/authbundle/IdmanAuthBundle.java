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
