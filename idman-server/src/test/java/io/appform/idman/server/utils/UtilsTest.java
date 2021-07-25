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

import com.google.common.base.Strings;
import io.appform.idman.model.AuthMode;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.db.model.ClientSession;
import io.appform.idman.model.TokenType;
import lombok.val;
import org.awaitility.Awaitility;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.InvalidJwtSignatureException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
class UtilsTest {

    @Test
    void hashedId() {
        assertEquals("5b56f40f-8828-301f-97fa-4511ddcd25fb", Utils.hashedId("TestString"));
    }

    @Test
    void readableId() {
        assertEquals("SOME_NAME", Utils.readableId("Some Name"));
        assertEquals("SOME_NAME", Utils.readableId("  Some #$!@$#$ Name "));
        assertEquals("_SOME_NAME_", Utils.readableId("  _some #$!@$#$ name_ "));
    }

    @Test
    void weekOfYear() {
        val week = Utils.weekOfYear();
        assertTrue(0 <= week);
        assertTrue(week <= 52);
    }

    @Test
    void testJWT() {
        val config = ServerTestingUtils.passwordauthConfig();
        val session = new ClientSession("SS1",
                                        "U1",
                                        "S1",
                                        "CS1",
                                        TokenType.DYNAMIC,
                                        null,
                                        false,
                                        new Date(),
                                        new Date());
        val jwt = Utils.createAccessToken(session, config.getJwt());
        assertFalse(Strings.isNullOrEmpty(jwt));

        val consumer = Utils.buildConsumer(config, "S1");
        try {
            val ctx = consumer.process(jwt);
            val claims = ctx.getJwtClaims();
            assertEquals("SS1", claims.getJwtId());
            assertEquals("U1", claims.getSubject());
            assertEquals(config.getJwt().getIssuerId(), claims.getIssuer());
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

        try {
            consumer.process(jwt + "_");
            fail("Supposed to fail the match");
        }
        catch (InvalidJwtSignatureException e) {
            assertTrue(e.getMessage().startsWith("JWT rejected due to invalid signature"));
        }
        catch (Exception e) {
            fail("Unexpected exception " + e);
        }
        try {
            consumer.process("_" + jwt);
            fail("Supposed to fail the match");
        }
        catch (InvalidJwtException e) {
            assertTrue(e.getMessage().startsWith("JWT processing failed."));
        }
        catch (Exception e) {
            fail("Unexpected exception " + e);
        }
    }

    @Test
    void testJWTError() {
        val config = ServerTestingUtils.passwordauthConfig();
        ClientSession session = new ClientSession("SS1", "U1", "S1", "CS1", TokenType.DYNAMIC,
                                                  null,
                                                  false,
                                                  new Date(),
                                                  new Date());
        val jwt = Utils.createAccessToken(session, config.getJwt());
        assertFalse(Strings.isNullOrEmpty(jwt));

        val consumer = Utils.buildConsumer(config, "S1");
        try {
            val ctx = consumer.process(jwt);
            val claims = ctx.getJwtClaims();
            assertEquals("SS1", claims.getJwtId());
            assertEquals("U1", claims.getSubject());
            assertEquals(config.getJwt().getIssuerId(), claims.getIssuer());
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

        try {
            consumer.process(jwt + "_");
            fail("Supposed to fail the match");
        }
        catch (InvalidJwtSignatureException e) {
            assertTrue(e.getMessage().startsWith("JWT rejected due to invalid signature"));
        }
        catch (Exception e) {
            fail("Unexpected exception " + e);
        }
        try {
            consumer.process("_" + jwt);
            fail("Supposed to fail the match");
        }
        catch (InvalidJwtException e) {
            assertTrue(e.getMessage().startsWith("JWT processing failed."));
        }
        catch (Exception e) {
            fail("Unexpected exception " + e);
        }
    }

    @Test
    void testJWTWithExpiry() {
        val now = new Date();
        val expiry = Utils.futureTime(io.dropwizard.util.Duration.seconds(3));
        val config = ServerTestingUtils.passwordauthConfig();
        config.setSessionDuration(io.dropwizard.util.Duration.milliseconds(3));
        ClientSession session = new ClientSession("SS1",
                                                  "U1",
                                                  "S1",
                                                  "CS1",
                                                  TokenType.DYNAMIC,
                                                  expiry,
                                                  false,
                                                  new Date(),
                                                  new Date());
        val jwt = Utils.createAccessToken(session, config.getJwt());
        assertFalse(Strings.isNullOrEmpty(jwt));

        val consumer = Utils.buildConsumer(config, "S1");
        try {
            val ctx = consumer.process(jwt);
            val claims = ctx.getJwtClaims();
            assertEquals("SS1", claims.getJwtId());
            assertEquals("U1", claims.getSubject());
            assertEquals(config.getJwt().getIssuerId(), claims.getIssuer());
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

        Awaitility.await()
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> new Date().after(expiry));
        try {
            consumer.process(jwt);
            fail("Supposed to fail the match");
        }
        catch (InvalidJwtException e) {
            assertTrue(e.getMessage().contains("The JWT is no longer valid"));
        }
        catch (Exception e) {
            fail("Unexpected exception " + e);
        }
    }

    @Test
    void sessionDuration() {
        val config = new AuthenticationConfig();
        config.setSessionDuration(io.dropwizard.util.Duration.days(1));
        assertEquals(io.dropwizard.util.Duration.days(1), Utils.sessionDuration(config));

        config.setSessionDuration(null);
        assertEquals(io.dropwizard.util.Duration.days(30), Utils.sessionDuration(config));
    }

    @Test
    void redirectionUrl() {
        val config = new AuthenticationConfig();
        config.setServer("http://myservice.test");
        assertEquals("http://myservice.test/oauth/callback/GOOGLE_AUTH",
                     Utils.redirectionUrl(AuthMode.GOOGLE_AUTH, config));
    }

    @Test
    void createUri() {
        assertEquals("http://localhost", Utils.createUri("http://localhost", "/"));
        assertEquals("http://localhost", Utils.createUri("http://localhost/", "/"));
        assertEquals("http://localhost/apis/test", Utils.createUri("http://localhost", "/apis/test"));
        assertEquals("http://localhost/apis/test", Utils.createUri("http://localhost/", "/apis/test"));
        assertEquals("http://localhost/apis/test", Utils.createUri("http://localhost/", "apis/test"));
        assertEquals("http://localhost/apis/test", Utils.createUri("http://localhost", "apis/test"));
    }
}