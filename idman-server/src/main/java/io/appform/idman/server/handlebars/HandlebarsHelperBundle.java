package io.appform.idman.server.handlebars;

import com.github.jknack.handlebars.Helper;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class HandlebarsHelperBundle<C extends Configuration> implements ConfiguredBundle<C> {

    protected abstract void configureHandlebars(C configuration);

    @Override
    public final void initialize(Bootstrap<?> bootstrap) {
        log.info("Initialized handlebars view renderer");
    }

    @Override
    public final void run(C configuration, Environment environment) {
        configureHandlebars(configuration);
    }

    public static <H> void registerHelperMissing(Helper<H> helper) {
        HandlebarsViewRenderer.HANDLEBARS.registerHelperMissing(helper);
    }

    public static <H> void registerHelper(String name, Helper<H> helper) {
        HandlebarsViewRenderer.HANDLEBARS.registerHelper(name, helper);
    }

    public static void registerHelpers(Object helperSource) {
        HandlebarsViewRenderer.HANDLEBARS.registerHelpers(helperSource);
    }

    public static void setPrettyPrint(boolean prettyPrint) {
        HandlebarsViewRenderer.HANDLEBARS.setPrettyPrint(prettyPrint);
    }

    public static void setInfiniteLoops(boolean infiniteLoops) {
        HandlebarsViewRenderer.HANDLEBARS.setInfiniteLoops(infiniteLoops);
    }
}
