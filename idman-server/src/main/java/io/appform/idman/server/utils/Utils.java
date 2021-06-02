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
import io.appform.idman.model.User;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.configs.JwtConfig;
import io.appform.idman.server.db.model.StoredUserSession;
import io.dropwizard.util.Duration;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.UUID;

/**
 *
 */
@UtilityClass
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
        ZoneId zoneId = ZoneId.of("Asia/Calcutta");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    @SneakyThrows
    public static String createJWT(final StoredUserSession session, final JwtConfig jwtConfig) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(jwtConfig.getIssuerId());
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setJwtId(session.getSessionId());
        claims.setNotBeforeMinutesInThePast(2);
        claims.setSubject(session.getUserId());
        claims.setAudience(session.getServiceId());

        if(null != session.getExpiry()) {
            claims.setExpirationTime(NumericDate.fromMilliseconds(session.getExpiry().getTime()));
        }
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        final byte[] secretKey = jwtConfig.getPrivateKey().getBytes(StandardCharsets.UTF_8);
        jws.setKey(new HmacKey(secretKey));
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA512);
        return jws.getCompactSerialization();
    }

    public static Duration sessionDuration(AuthenticationConfig authConfig) {
        final Duration dynamicSessionDuration = authConfig.getSessionDuration();
        return dynamicSessionDuration != null
               ? dynamicSessionDuration
               : Duration.days(30);
    }

    public static String redirectionUrl(AuthMode authMode, AuthenticationConfig authenticationConfig) {
        return  authenticationConfig.getServer() + "/oauth/callback/" + authMode.name();
    }

    public static JwtConsumer buildConsumer(AuthenticationConfig authConfig, final String serviceId) {
        val jwtConfig = authConfig.getJwt();
        final byte[] secretKey = jwtConfig.getPrivateKey().getBytes(StandardCharsets.UTF_8);
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
}
