package io.appform.idman.server.auth.configs;

import com.google.common.annotations.VisibleForTesting;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

/**
 *
 */
@Data
@NoArgsConstructor
public class JwtConfig {
    @NotEmpty
    private String privateKey;

    @NotEmpty
    private String issuerId;

    @NotEmpty
    private String domain;

    @NotEmpty
    private String authCachePolicy = "maximumSize=10000, expireAfterAccess=10m";

    @VisibleForTesting
    public JwtConfig(String privateKey, String issuerId) {
        this.privateKey = privateKey;
        this.issuerId = issuerId;
    }
}
