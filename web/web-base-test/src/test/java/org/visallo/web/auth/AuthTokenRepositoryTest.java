package org.visallo.web.auth;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;

import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;
import static org.visallo.core.config.Configuration.AUTH_TOKEN_PASSWORD;
import static org.visallo.core.config.Configuration.AUTH_TOKEN_SALT;

public class AuthTokenRepositoryTest {
    private static final String PASSWORD = "password";
    private static final String SALT = "salt";

    private AuthTokenRepository authTokenRepository;

    @Before
    public void before() {
        Map config = ImmutableMap.of(AUTH_TOKEN_PASSWORD, PASSWORD, AUTH_TOKEN_SALT, SALT);
        HashMapConfigurationLoader configLoader = new HashMapConfigurationLoader(config);
        authTokenRepository = new AuthTokenRepository(new Configuration(configLoader, config), null);
    }

    @Test
    public void testValidTokenCreationAndValidation() throws Exception {
        String userid = "userid";
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        AuthToken token = new AuthToken(userid, expiration, true, AuthTokenUse.WEB);
        String tokenText = authTokenRepository.serialize(token);
        AuthToken parsedToken = authTokenRepository.parse(tokenText);
        assertEquals(userid, parsedToken.getUserId());
        assertTrue("Dates aren't close enough to each other",
                Math.abs(expiration.getTime() - parsedToken.getExpiration().getTime()) < 1000);
    }

    @Test
    public void testTokenReportsExpirationCorrectly() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() - 10);
        AuthToken token = new AuthToken("userid", expiration, true, AuthTokenUse.WEB);
        String tokenText = authTokenRepository.serialize(token);
        AuthToken parsedToken = authTokenRepository.parse(tokenText);
        assertTrue(parsedToken.isValid(60));
        assertFalse(parsedToken.isValid(0));
    }

    @Test
    public void testTokenValidationFailure() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        String userid = "userid";
        AuthToken token = new AuthToken(userid, expiration, true, AuthTokenUse.WEB);
        String tokenText = authTokenRepository.serialize(token);

        AuthToken parsedToken = authTokenRepository.parse(tokenText + "a");
        assertFalse(parsedToken.isVerified());
        assertFalse(parsedToken.isValid(60));
        assertEquals(userid, parsedToken.getUserId());
    }

    @Test
    public void testNewTokenContainsIdClaim() {
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        AuthToken token = new AuthToken("userid", expiration, true, AuthTokenUse.WEB);
        assertNotNull(token.getTokenId());
    }
}
