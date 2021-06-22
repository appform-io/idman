package io.appform.idman.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/**
 *
 */
@Value
public class TokenInfo {
    @JsonProperty("access_token")
    String accessToken;
    @JsonProperty("refresh_token")
    String refreshToken;
    @JsonProperty("expires_in")
    long expiry;
    @JsonProperty("token_type")
    String type;
    @JsonProperty("scope")
    String role;
    @JsonProperty("user")
    IdmanUser user;
}
