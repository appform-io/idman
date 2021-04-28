package io.appform.idman.server.db.impl;

import com.google.common.collect.Maps;
import io.dropwizard.db.DataSourceFactory;
import lombok.experimental.UtilityClass;

import java.util.Map;

/**
 *
 */
@UtilityClass
public class TestUtils {
    public static DataSourceFactory createConfig() {
        Map<String, String> properties = Maps.newHashMap();
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "create");
        properties.put("hibernate.show_sql", "true");
        properties.put("hibernate.format_sql", "true");

        DataSourceFactory shard = new DataSourceFactory();
        shard.setDriverClass("org.h2.Driver");
        shard.setUrl("jdbc:h2:mem:idman");
        shard.setValidationQuery("select 1");
        shard.setProperties(properties);

        return shard;
    }
}
