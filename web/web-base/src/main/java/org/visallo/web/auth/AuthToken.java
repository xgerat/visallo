package org.visallo.web.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

public class AuthToken {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String DESCRIPTION_CLAIM = "description";

    private final String userId;
    private final SecretKey jwtKey;
    private final Date expiration;
    private final String tokenId;
    private final String description;
    private final boolean verified;
    private final AuthTokenUse usage;

    public AuthToken(String userId, SecretKey macKey, Date expiration, boolean verified, AuthTokenUse usage) {
        this(AuthToken.generateTokenId(), userId, macKey, expiration, verified, null, usage);
    }

    public AuthToken(String userId, SecretKey macKey, Date expiration, boolean verified, String description, AuthTokenUse usage) {
        this(AuthToken.generateTokenId(), userId, macKey, expiration, verified, description, usage);
    }

    private AuthToken(String tokenId, String userId, SecretKey macKey, Date expiration, boolean verified, String description, AuthTokenUse usage) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.jwtKey = macKey;
        this.expiration = expiration;
        this.verified = verified;
        this.description = description;
        this.usage = usage;
    }

    public static SecretKey generateKey(String keyPassword, String keySalt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(keyPassword.toCharArray(), keySalt.getBytes(), 10000, 256);
        return factory.generateSecret(spec);
    }

    public static AuthToken parse(String token, SecretKey macKey) throws AuthTokenException {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(macKey);
            boolean verified = signedJWT.verify(verifier);

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            AuthTokenUse usage = claims.getAudience().contains(AuthTokenUse.API.name()) ? AuthTokenUse.API : AuthTokenUse.WEB;
            return new AuthToken(
                    claims.getJWTID(),
                    claims.getSubject(),
                    macKey,
                    claims.getExpirationTime(),
                    verified,
                    claims.getStringClaim(DESCRIPTION_CLAIM),
                    usage);
        } catch (Exception e) {
            throw new AuthTokenException(e);
        }
    }

    public String serialize() throws AuthTokenException {
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .jwtID(tokenId)
                .subject(userId)
                .expirationTime(expiration)
                .audience(usage.name());

        if (description != null) {
            claimsBuilder.claim(DESCRIPTION_CLAIM, description);
        }

        try {
            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build());
            JWSSigner signer = new MACSigner(jwtKey);
            signedJwt.sign(signer);
            return signedJwt.serialize();
        } catch (Exception e) {
            throw new AuthTokenException(e);
        }
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getUserId() {
        return userId;
    }

    public Date getExpiration() {
        return expiration;
    }

    public AuthTokenUse getUsage() {
        return usage;
    }

    public String getDescription() {
        return description;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isValid(int toleranceInSeconds) {
        return verified && !isExpired(toleranceInSeconds);
    }

    public boolean isExpired(int toleranceInSeconds) {
        Calendar expirationWithTolerance = Calendar.getInstance();
        expirationWithTolerance.setTime(expiration);
        expirationWithTolerance.add(Calendar.SECOND, toleranceInSeconds);
        return expirationWithTolerance.getTime().before(new Date());
    }

    private static String generateTokenId() {
        byte[] randomBytes = new byte[128];
        SECURE_RANDOM.nextBytes(randomBytes);

        ByteBuffer currentTimeBuffer = ByteBuffer.allocate(Long.BYTES);
        currentTimeBuffer.putLong(System.currentTimeMillis());

        return Base64.getEncoder().encodeToString(randomBytes)
                + "@" + Base64.getEncoder().encodeToString(currentTimeBuffer.array());
    }
}
