package io.appform.idman.server;

import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class AppConfig extends Configuration {
    @NotNull
    @Valid
    private DataSourceFactory db = new DataSourceFactory();

    private AuthenticationConfig authenticationCore;
}
