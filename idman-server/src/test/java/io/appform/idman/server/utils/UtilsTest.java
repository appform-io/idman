package io.appform.idman.server.utils;

import com.google.common.base.Strings;
import io.appform.idman.model.AuthMode;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.db.model.SessionType;
import io.appform.idman.server.db.model.StoredUserSession;
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
        val config = TestingUtils.passwordauthConfig();
        val session = new StoredUserSession("SS1", "U1", "S1", "CS1", SessionType.DYNAMIC, null);
        val jwt = Utils.createJWT(session, config.getJwt());
        assertFalse(Strings.isNullOrEmpty(jwt));

        val consumer = Utils.buildConsumer(config, "S1");
        try {
            val ctx = consumer.process(jwt);
            val claims = ctx.getJwtClaims();
            assertEquals("SS1", claims.getJwtId());
            assertEquals("U1", claims.getSubject());
            assertEquals(config.getJwt().getIssuerId(), claims.getIssuer());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        try {
            consumer.process(jwt + "_");
            fail("Supposed to fail the match");
        } catch (InvalidJwtSignatureException e) {
            assertTrue(e.getMessage().startsWith("JWT rejected due to invalid signature"));
        }
        catch (Exception e) {
            fail("Unexpected exception " + e);
        }
        try {
            consumer.process("_" + jwt);
            fail("Supposed to fail the match");
        } catch (InvalidJwtException e) {
            assertTrue(e.getMessage().startsWith("JWT processing failed."));
        }
        catch (Exception e) {
            fail("Unexpected exception " + e);
        }
    }

    @Test
    void testJWTError() {
        val config = TestingUtils.passwordauthConfig();
        val session = new StoredUserSession("SS1", "U1", "S1", "CS1", SessionType.DYNAMIC, null);
        val jwt = Utils.createJWT(session, config.getJwt());
        assertFalse(Strings.isNullOrEmpty(jwt));

        val consumer = Utils.buildConsumer(config, "S1");
        try {
            val ctx = consumer.process(jwt);
            val claims = ctx.getJwtClaims();
            assertEquals("SS1", claims.getJwtId());
            assertEquals("U1", claims.getSubject());
            assertEquals(config.getJwt().getIssuerId(), claims.getIssuer());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        try {
            consumer.process(jwt + "_");
            fail("Supposed to fail the match");
        } catch (InvalidJwtSignatureException e) {
            assertTrue(e.getMessage().startsWith("JWT rejected due to invalid signature"));
        }
        catch (Exception e) {
            fail("Unexpected exception " + e);
        }
        try {
            consumer.process("_" + jwt);
            fail("Supposed to fail the match");
        } catch (InvalidJwtException e) {
            assertTrue(e.getMessage().startsWith("JWT processing failed."));
        }
        catch (Exception e) {
            fail("Unexpected exception " + e);
        }
    }

    @Test
    void testJWTWithExpiry() {
        val now = new Date();
        val expiry = new Date(now.getTime() + 3_000);
        val config = TestingUtils.passwordauthConfig();
        val session = new StoredUserSession("SS1", "U1", "S1", "CS1", SessionType.DYNAMIC, expiry);
        val jwt = Utils.createJWT(session, config.getJwt());
        assertFalse(Strings.isNullOrEmpty(jwt));

        val consumer = Utils.buildConsumer(config, "S1");
        try {
            val ctx = consumer.process(jwt);
            val claims = ctx.getJwtClaims();
            assertEquals("SS1", claims.getJwtId());
            assertEquals("U1", claims.getSubject());
            assertEquals(config.getJwt().getIssuerId(), claims.getIssuer());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        Awaitility.await()
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> new Date().after(expiry));
        try {
            consumer.process(jwt);
            fail("Supposed to fail the match");
        } catch (InvalidJwtException e) {
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
        config.setServer("myservice.test");
        assertEquals("http://myservice.test/oauth/callback/GOOGLE_AUTH",
                     Utils.redirectionUrl(AuthMode.GOOGLE_AUTH, config));
        config.setSecureEndpoint(true);
        assertEquals("https://myservice.test/oauth/callback/GOOGLE_AUTH",
                     Utils.redirectionUrl(AuthMode.GOOGLE_AUTH, config));
    }
}