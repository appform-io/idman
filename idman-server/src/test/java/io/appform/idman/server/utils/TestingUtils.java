package io.appform.idman.server.utils;

import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.configs.JwtConfig;
import io.dropwizard.util.Duration;
import lombok.experimental.UtilityClass;
import lombok.val;

/**
 *
 */
@UtilityClass
public class TestingUtils {
    public static AuthenticationConfig passwordauthConfig() {
        val authenticationConfig = new AuthenticationConfig();
        authenticationConfig.setDomain("testd");
        authenticationConfig.setServer("localhost");
        authenticationConfig.setSessionDuration(Duration.days(7));

        val jwtConfig = new JwtConfig();
        jwtConfig.setIssuerId("testissuer");
        jwtConfig.setPrivateKey(
                "bYdNUUyCqx8IuGNqhFYS27WizZrfupAmJS8I4mfj2Cjox9Nc04Oews9tJEiDTrJfopzKdjygi8SgXeopSe/rPYqEKfrAUw/Dn6wMVhE56S7/5DKAvYusA2fQRqxOrOosO1lERnArw15tkAf/z5QUUUXnKZZTiczNEebjs2OG5s94PGxtQzxtYsZ1q2oXoq4lKPTosPpwkRxeh8LQCweDGR80xgoM1+yDAoYIeg==");
        authenticationConfig.setJwt(jwtConfig);
        return authenticationConfig;
    }
}
