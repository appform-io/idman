package io.appform.idman.client;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.Collections;
import java.util.Set;

/**
 *
 */
@Data
public class IdmanClientConfig {

    @NotEmpty
    private String serviceId;

    @NotEmpty
    private String authEndpoint;

    private Set<String> allowedPaths = Collections.emptySet();

    private String cacheSpec = "maximumSize=10000, expireAfterAccess=10m";

    String resourcePrefix;

//    String redirectionEndpoint;
}