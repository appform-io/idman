package io.appform.idman.client;

import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.TokenInfo;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.Collections;

/**
 *
 */
@UtilityClass
public class ClientTestingUtils {
    public static IdmanClientConfig clientConfig() {
        val clientConfig = new IdmanClientConfig();
        clientConfig.setAuthEndpoint("http://localhost:8080");
        clientConfig.setPublicEndpoint("http://localhost:3000");
        clientConfig.setResourcePrefix("/apis");
        clientConfig.setServiceId("testservice");
        clientConfig.setAllowedPaths(Collections.singleton("unchecked"));
        return clientConfig;
    }

    public static TokenInfo tokenInfo(String token, IdmanUser user) {
        return new TokenInfo(token, token, 60, "bearer", user.getRole(), user);
    }
}
