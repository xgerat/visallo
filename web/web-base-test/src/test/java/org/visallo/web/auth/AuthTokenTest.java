package org.visallo.web.auth;

import org.junit.Test;

import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import static org.junit.Assert.*;

public class AuthTokenTest {
    @Test
    public void testGeneratedKeysEqualWhenPasswordAndSaltAreTheSame() throws Exception {
        String password = "the password";
        String salt = "the salt";
        assertEquals(AuthToken.generateKey(password, salt), AuthToken.generateKey(password, salt));
    }

    @Test
    public void testGeneratedKeysNotEqualWhenPasswordAndSaltAreDifferent() throws InvalidKeySpecException, NoSuchAlgorithmException {
        String password = "the password";
        String salt = "the salt";
        assertNotEquals(AuthToken.generateKey(password, salt), AuthToken.generateKey(password + "2", salt));
        assertNotEquals(AuthToken.generateKey(password, salt), AuthToken.generateKey(password, salt + "2"));
        assertNotEquals(AuthToken.generateKey(password, salt), AuthToken.generateKey(password + "2", salt + "2"));
    }

    @Test
    public void testValidTokenCreationAndValidation() throws Exception {
        SecretKey key = AuthToken.generateKey("password", "salt");
        String userid = "userid";
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        AuthToken token = new AuthToken(userid, key, expiration, true, AuthTokenUse.WEB);
        String tokenText = token.serialize();
        AuthToken parsedToken = AuthToken.parse(tokenText, key);
        assertEquals(userid, parsedToken.getUserId());
        assertTrue("Dates aren't close enough to each other",
                Math.abs(expiration.getTime() - parsedToken.getExpiration().getTime()) < 1000);
    }

    @Test
    public void testTokenReportsExpirationCorrectly() throws Exception {
        SecretKey key = AuthToken.generateKey("password", "salt");
        Date expiration = new Date(System.currentTimeMillis() - 10);
        AuthToken token = new AuthToken("userid", key, expiration, true, AuthTokenUse.WEB);
        String tokenText = token.serialize();
        AuthToken parsedToken = AuthToken.parse(tokenText, key);
        assertTrue(parsedToken.isValid(60));
        assertFalse(parsedToken.isValid(0));
    }

    @Test
    public void testTokenValidationFailure() throws Exception {
        SecretKey key = AuthToken.generateKey("password", "salt");
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        String userid = "userid";
        AuthToken token = new AuthToken(userid, key, expiration, true, AuthTokenUse.WEB);
        String tokenText = token.serialize();

        AuthToken parsedToken = AuthToken.parse(tokenText + "a", key);
        assertFalse(parsedToken.isVerified());
        assertFalse(parsedToken.isValid(60));
        assertEquals(userid, parsedToken.getUserId());
    }

    @Test
    public void testNewTokenContainsIdClaim() throws InvalidKeySpecException, NoSuchAlgorithmException {
        SecretKey key = AuthToken.generateKey("password", "salt");
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        AuthToken token = new AuthToken("userid", key, expiration, true, AuthTokenUse.WEB);
        assertNotNull(token.getTokenId());
    }
}
