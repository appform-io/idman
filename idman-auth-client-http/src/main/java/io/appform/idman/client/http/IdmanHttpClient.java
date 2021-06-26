/*
 * Copyright 2021. Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package io.appform.idman.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.appform.idman.client.IdManClient;
import io.appform.idman.model.TokenInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 *
 */
@Slf4j
public class IdmanHttpClient extends IdManClient {
    private final CloseableHttpClient httpClient;
    private final IdManHttpClientConfig clientConfig;
    private final ObjectMapper mapper;


    public IdmanHttpClient(final IdManHttpClientConfig clientConfig, ObjectMapper mapper) {
        this.clientConfig = clientConfig;
        this.mapper = mapper;
        val connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(clientConfig.getMaxClientConnections());
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

    }

    @Override
    @SneakyThrows
    public Optional<TokenInfo> accessToken(String serviceId, String tokenId) {
        return oauthTokenApiCall(tokenId, "authorization_code", "code");
    }


    @Override
    @SneakyThrows
    protected Optional<TokenInfo> validateTokenImpl(String serviceId, String token) {
        return oauthTokenApiCall(token, "refresh_token", "refresh_token");
    }

    @Override
    public boolean deleteToken(String serviceId, String jwt) {
        return false;
    }

    private Optional<TokenInfo> oauthTokenApiCall(String code, String grantType, String paramName) throws UnsupportedEncodingException {
        val requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
                .setConnectionRequestTimeout(clientConfig.getRequestTimeoutMs())
                .setConnectTimeout(clientConfig.getConnectionTimeoutMs())
                .build();

        val url = String.format("%s/apis/oauth2/token", clientConfig.getAuthEndpoint());
        log.debug("Token API URL: {}", url);
        val params = ImmutableList.<NameValuePair>builder()
                .add(new BasicNameValuePair(paramName, code))
                .add(new BasicNameValuePair("client_id", clientConfig.getServiceId()))
                .add(new BasicNameValuePair("client_secret", clientConfig.getAuthSecret()))
                .add(new BasicNameValuePair("grant_type", grantType))
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(requestConfig);
        post.setEntity(new UrlEncodedFormEntity(params));
        log.debug("Headers: {}", Arrays.toString(post.getAllHeaders()));
        log.debug("Entity: {}", post.getEntity());
        log.debug("Method: {}", post.getMethod());
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            val statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                val s = new String(EntityUtils.toByteArray(response.getEntity()), StandardCharsets.UTF_8);
                log.debug("Server response: {}", s);
                return Optional.of(mapper.readValue(s, TokenInfo.class));
            }
            log.error("Error returned by check api: {}", statusCode);
        }
        catch (Exception e) {
            log.error("Error calling auth api: " + url, e);
        }
        return Optional.empty();
    }
}
