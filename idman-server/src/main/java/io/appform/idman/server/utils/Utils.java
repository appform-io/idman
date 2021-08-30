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

package io.appform.idman.server.utils;

import io.appform.idman.model.AuthMode;
import io.appform.idman.model.TokenType;
import io.appform.idman.model.User;
import io.appform.idman.server.auth.ParsedTokenInfo;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.configs.JwtConfig;
import io.appform.idman.server.db.model.ClientSession;
import io.dropwizard.util.Duration;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 *
 */
@UtilityClass
@Slf4j
public class Utils {
    public static String hashedId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static String readableId(String value) {
        return value
                .trim()
                .replaceAll("\\p{Blank}", "_")
                .replaceAll("[\\p{Punct}&&[^_]]", "")
                .replaceAll("_{2,}", "_")
                .toUpperCase();
    }

    public static int weekOfYear() {
        return ZonedDateTime.now(ZoneId.of("Asia/Calcutta")).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    @SneakyThrows
    public static String createAccessToken(final ClientSession session, final JwtConfig jwtConfig) {
        val claims = new JwtClaims();
        claims.setIssuer(jwtConfig.getIssuerId());
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setJwtId(session.getSessionId());
        claims.setNotBeforeMinutesInThePast(2);
        claims.setSubject(session.getUserId());
        claims.setAudience(session.getServiceId());
        if (null != session.getCreated()) {
            claims.setIssuedAt(NumericDate.fromMilliseconds(session.getCreated().getTime()));
        }
        else {
            claims.setIssuedAt(NumericDate.now());
        }
        if (null != session.getExpiry()) {
            claims.setExpirationTime(NumericDate.fromMilliseconds(session.getExpiry().getTime()));
        }
        val jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        val secretKey = jwtConfig.getPrivateKey().getBytes(StandardCharsets.UTF_8);
        jws.setKey(new HmacKey(secretKey));
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA512);
        return jws.getCompactSerialization();
    }

    public Optional<ParsedTokenInfo> parseToken(String token, JwtConsumer jwtConsumer) {
        final String userId;
        final String sessionId;
        final String extServiceId;
        final NumericDate expiry;
        try {

            val jwtContext = jwtConsumer.process(token);

            val claims = jwtContext.getJwtClaims();
            userId = claims.getSubject();
            sessionId = claims.getJwtId();
            extServiceId = claims.getAudience().get(0);
            expiry = claims.getExpirationTime();
        }
        catch (MalformedClaimException | InvalidJwtException e) {
            log.error("exception in claim extraction {}. Token: {}", e.getMessage(), token);
            return Optional.empty();
        }
        return Optional.of(new ParsedTokenInfo(userId,
                                               sessionId,
                                               extServiceId,
                                               expiry,
                                               null == expiry
                                               ? TokenType.STATIC
                                               : TokenType.DYNAMIC));
    }

    public static Duration sessionDuration(AuthenticationConfig authConfig) {
        val dynamicSessionDuration = authConfig.getSessionDuration();
        return dynamicSessionDuration != null
               ? dynamicSessionDuration
               : Duration.days(30);
    }

    public static String redirectionUrl(AuthMode authMode, AuthenticationConfig authenticationConfig) {
        return authenticationConfig.getServer() + "/oauth/callback/" + authMode.name();
    }

    public static JwtConsumer buildConsumer(AuthenticationConfig authConfig, final String serviceId) {
        return buildConsumer(authConfig.getJwt(), serviceId);
    }

    public static JwtConsumer buildConsumer(JwtConfig jwtConfig, final String serviceId) {
        val secretKey = jwtConfig.getPrivateKey().getBytes(StandardCharsets.UTF_8);
        return new JwtConsumerBuilder()
                .setRequireIssuedAt()
                .setRequireSubject()
                .setExpectedIssuer(jwtConfig.getIssuerId())
                .setVerificationKey(new HmacKey(secretKey))
                .setJwsAlgorithmConstraints(new AlgorithmConstraints(
                        AlgorithmConstraints.ConstraintType.WHITELIST,
                        AlgorithmIdentifiers.HMAC_SHA512))
                .setExpectedAudience(serviceId)
                .build();
    }

    public static User toWire(io.appform.idman.server.db.model.StoredUser user) {
        return new User(user.getUserId(),
                        user.getName(),
                        user.getUserType(),
                        user.getAuthState().getAuthMode());
    }

    public static String createUri(String host, String path) {
        val cleanedHost = host.replaceAll("/$", "");
        if (path.equals("/")) {
            return cleanedHost;
        }
        return cleanedHost + (path.startsWith("/")
                              ? path
                              : "/" + path);

    }

    public static Date futureTime(Duration sessionDuration) {
        return Date.from(Instant.now().plus(sessionDuration.toMilliseconds(), ChronoUnit.MILLIS));
    }
}
