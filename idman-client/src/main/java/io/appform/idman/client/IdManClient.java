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

package io.appform.idman.client;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Strings;
import io.appform.idman.model.TokenInfo;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Abstraction for client information. Caches auth for 5 mins.
 */
@Slf4j
public abstract class IdManClient {


    private final LoadingCache<CacheKey, TokenInfo> localCache;

    protected IdManClient() {
        this.localCache = Caffeine.newBuilder()
                .maximumSize(1)
                .refreshAfterWrite(1, TimeUnit.MINUTES)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build(new CacheLoader<CacheKey, TokenInfo>() {
                    @Nullable
                    @Override
                    public TokenInfo load(@NonNull CacheKey key) {
                        log.debug("Actual Auth called");
                        val tokenInfo = refreshAccessTokenImpl(key.getServiceId(), key.getToken()).orElse(null);
                        if(null == tokenInfo) {
                            return null;
                        }
                        return tokenInfo;
                    }
                });
    }

    public abstract Optional<TokenInfo> accessToken(String serviceId, String tokenId);

    public Optional<TokenInfo> refreshAccessToken(String serviceId, String token) {
        log.info("Authenticator called. Service ID: {} Token: {}", serviceId, token);
        if (Strings.isNullOrEmpty(serviceId) || Strings.isNullOrEmpty(token)) {
            return Optional.empty();
        }
        return Optional.ofNullable(localCache.get(new CacheKey(token, serviceId)));

    }

    protected abstract Optional<TokenInfo> refreshAccessTokenImpl(String serviceId, String token);

    @Value
    private static class CacheKey {
        String token;
        String serviceId;
    }
}
