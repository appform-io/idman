package io.appform.idman.client;

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
        clientConfig.setResourcePrefix("/apis");
        clientConfig.setServiceId("testservice");
        clientConfig.setAllowedPaths(Collections.singleton("unchecked"));
        return clientConfig;
    }
}
