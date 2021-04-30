package io.appform.idman.server.utils;

import com.google.common.base.Charsets;
import io.appform.idman.model.AuthMode;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.configs.JwtConfig;
import io.appform.idman.server.db.model.StoredUserSession;
import io.dropwizard.util.Duration;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
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
        return UUID.nameUUIDFromBytes(value.getBytes(Charsets.UTF_8)).toString();
    }

    public static String readableId(String value) {
        return value
                .trim()
                .replaceAll("\\p{Blank}", "_")
                .replaceAll("[^a-zA-Z0-9_]", "")
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
        return  (authenticationConfig.isSecureEndpoint() ? "https" : "http")
                + "://"
                + authenticationConfig.getServer()
                + "/oauth/callback/" + authMode.name();
    }
}
