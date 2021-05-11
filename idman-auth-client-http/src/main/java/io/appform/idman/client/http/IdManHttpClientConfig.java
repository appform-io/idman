package io.appform.idman.client.http;

import io.appform.idman.client.IdmanClientConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IdManHttpClientConfig extends IdmanClientConfig {

    @Min(0)
    @Max(30_000)
    private int connectionTimeoutMs = 1_000;

    @Min(0)
    @Max(30_000)
    private int requestTimeoutMs = 1_000;

    @Min(5)
    @Max(128)
    private int maxClientConnections = 5;

    @NotEmpty
    private String authSecret;
}
