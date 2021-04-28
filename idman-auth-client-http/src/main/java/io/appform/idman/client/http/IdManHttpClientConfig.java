package io.appform.idman.client.http;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

/**
 *
 */
@Data
public class IdManHttpClientConfig {
    @NotEmpty
    private String host;

    @Min(0)
    @Max(65535)
    private int port;

    @Min(0)
    @Max(30_000)
    private int connectionTimeoutMs = 1_000;

    @Min(0)
    @Max(30_000)
    private int requestTimeoutMs = 1_000;

    @Min(5)
    @Max(128)
    private int maxClientConnections = 5;

    private boolean insecure;

    @NotEmpty
    private String serviceId;

    @NotEmpty
    private String authSecret;
}
