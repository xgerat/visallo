package org.visallo.web.auth;

import com.google.inject.Inject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.vertexium.TextIndexHint;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyPropertyDefinition;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.types.StringVisalloProperty;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.PropertyType;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.visallo.core.model.ontology.OntologyRepository.PUBLIC;

public class AuthTokenRepository {
    private static final StringVisalloProperty API_TOKEN_PROPERTY = new StringVisalloProperty("http://visallo.org/user#apiToken");
    private static final String DESCRIPTION_CLAIM = "description";

    private final SecretKey tokenSigningKey;
    private final UserRepository userRepository;

    private final int tokenExpirationToleranceInSeconds;

    @Inject
    public AuthTokenRepository(
            Configuration configuration,
            UserRepository userRepository,
            OntologyRepository ontologyRepository
    ) {
        this.userRepository = userRepository;

        ensureApiTokenUserPropertyDefined(ontologyRepository);

        tokenExpirationToleranceInSeconds = configuration.getInt(Configuration.AUTH_TOKEN_EXPIRATION_TOLERANCE_IN_SECS);

        String keyPassword = configuration.get(Configuration.AUTH_TOKEN_PASSWORD, null);
        String keySalt = configuration.get(Configuration.AUTH_TOKEN_SALT, null);

        try {
            tokenSigningKey = generateKey(keyPassword, keySalt);
        } catch (Exception e) {
            throw new VisalloException("Unable to generate token signing key");
        }
    }

    public SecretKey generateKey(String keyPassword, String keySalt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(keyPassword.toCharArray(), keySalt.getBytes(), 10000, 256);
        return factory.generateSecret(spec);
    }

    public AuthToken parse(String token) throws AuthTokenException {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(tokenSigningKey);
            boolean verified = signedJWT.verify(verifier);

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            AuthTokenUse usage = claims.getAudience().contains(AuthTokenUse.API.name()) ? AuthTokenUse.API : AuthTokenUse.WEB;
            return new AuthToken(
                    claims.getJWTID(),
                    claims.getSubject(),
                    claims.getExpirationTime(),
                    verified,
                    claims.getStringClaim(DESCRIPTION_CLAIM),
                    usage);
        } catch (Exception e) {
            throw new AuthTokenException(e);
        }
    }

    public String serialize(AuthToken authToken) throws AuthTokenException {
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .jwtID(authToken.getTokenId())
                .subject(authToken.getUserId())
                .expirationTime(authToken.getExpiration())
                .audience(authToken.getUsage().name());

        if (authToken.getDescription() != null) {
            claimsBuilder.claim(DESCRIPTION_CLAIM, authToken.getDescription());
        }

        try {
            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build());
            JWSSigner signer = new MACSigner(tokenSigningKey);
            signedJwt.sign(signer);
            return signedJwt.serialize();
        } catch (Exception e) {
            throw new AuthTokenException(e);
        }
    }

    public boolean isValid(AuthToken authToken) {
        User user = userRepository.findById(authToken.getUserId());
        return user != null && isValid(user, authToken);
    }

    public boolean isValid(User user, AuthToken authToken) {
        checkNotNull(user, "Token validity cannot be checked without a user.");

        boolean valid = authToken.isVerified() && !authToken.isExpired(tokenExpirationToleranceInSeconds);
        if (valid && authToken.getUsage() == AuthTokenUse.API) {
            valid = loadValidApiTokens(user).stream().anyMatch(userToken -> userToken.getTokenId().equals(authToken.getTokenId()));
        }

        return valid;
    }

    public void saveApiToken(User user, AuthToken authToken) throws AuthTokenException {
        userRepository.setPropertyOnUser(user, authToken.getDescription(), API_TOKEN_PROPERTY.getPropertyName(), serialize(authToken));
    }

    public void deleteApiToken(User user, String apiTokenId) {
        loadValidApiTokens(user).forEach(apiToken -> {
            if (apiToken.getTokenId().equals(apiTokenId)) {
                userRepository.removePropertyFromUser(user, apiToken.getDescription(), API_TOKEN_PROPERTY.getPropertyName());
            }
        });
    }

    public List<AuthToken> loadValidApiTokens(User user) {
        Map<String, Object> tokens = user.getCustomProperties().get(API_TOKEN_PROPERTY.getPropertyName());
        if (tokens != null) {
            return tokens.values().stream().map(serializedToken -> {
                try {
                    return parse((String) serializedToken);
                } catch (AuthTokenException e) {
                    throw new VisalloException("Unable to parse token " + serializedToken + " for user " + user.getUserId());
                }
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void ensureApiTokenUserPropertyDefined(OntologyRepository ontologyRepository) {
        List<Concept> concepts = new ArrayList<>();
        concepts.add(ontologyRepository.getConceptByIRI(UserRepository.USER_CONCEPT_IRI, PUBLIC));
        OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(
                concepts,
                API_TOKEN_PROPERTY.getPropertyName(),
                "API Token",
                PropertyType.STRING
        );
        propertyDefinition.setUserVisible(false);
        propertyDefinition.setTextIndexHints(TextIndexHint.NONE);
        ontologyRepository.getOrCreateProperty(propertyDefinition, userRepository.getSystemUser(), PUBLIC);
    }
}
