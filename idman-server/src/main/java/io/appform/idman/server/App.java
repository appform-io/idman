package io.appform.idman.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.Stage;
import io.appform.idman.server.db.model.*;
import io.appform.idman.server.handlebars.HandlebarsHelperBundle;
import io.appform.idman.server.handlebars.HandlebarsHelpers;
import io.appform.idman.server.handlebars.HandlebarsViewRenderer;
import io.appform.idman.server.modules.CoreModule;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.val;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import ru.vyarus.guicey.gsp.ServerPagesBundle;

/**
 *
 */
public class App extends Application<AppConfig> {

    private HibernateBundle<AppConfig> hibernate
            = new HibernateBundle<AppConfig>(StoredUser.class,
                                             StoredPassword.class,
                                             StoredService.class,
                                             StoredRole.class,
                                             StoredRole.class,
                                             StoredServiceUserRole.class,
                                             StoredUser.class,
                                             StoredUserAuthState.class,
                                             StoredUserSession.class) {
        @Override
        public PooledDataSourceFactory getDataSourceFactory(AppConfig appConfig) {
            return appConfig.getDb();
        }
    };

    private HandlebarsHelperBundle<AppConfig> handlebarsBundle = new HandlebarsHelperBundle<AppConfig>() {
        @Override
        protected void configureHandlebars(AppConfig configuration) {
        }
    };

    @Override
    public void initialize(Bootstrap<AppConfig> bootstrap) {
        super.initialize(bootstrap);
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                                               new EnvironmentVariableSubstitutor(true)));
        bootstrap.addBundle(hibernate);

        /*
                bootstrap.addBundle(new AssetsBundle("/assets", "/static"));
        bootstrap.addBundle(new ViewBundle<>());
        bootstrap.addBundle(handlebarsBundle);*/
        bootstrap.addBundle(
                GuiceBundle.<AppConfig>builder()
                        .enableAutoConfig("io.appform.idman.server.resources", "io.appform.idman.authbundle")
                        .modules(new CoreModule(hibernate))
                        .bundles(ServerPagesBundle.builder()
                                         .addViewRenderers(new HandlebarsViewRenderer())
                                         .build())
                        .bundles(ServerPagesBundle.app("ui", "/assets/", "/")
                                         .mapViews("/ui")
                                         .requireRenderers("handlebars")
                                         .build())
                        .printDiagnosticInfo()
                        .build(Stage.PRODUCTION));

        HandlebarsHelperBundle.registerHelpers(new HandlebarsHelpers());
    }

    @Override
    public void run(AppConfig configuration, Environment environment) throws Exception {
        val objectMapper = environment.getObjectMapper();
        objectMapper.registerModule(new ParameterNamesModule());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static void main(String[] args) throws Exception {
        val app = new App();
        app.run(args);
    }
}
