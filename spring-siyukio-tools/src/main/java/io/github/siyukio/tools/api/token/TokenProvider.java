package io.github.siyukio.tools.api.token;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.JWTClaimsSet;
import io.github.siyukio.tools.util.CryptoUtils;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * @author Buddy
 */
@Slf4j
public final class TokenProvider {

    private final Duration accessTokenDuration;

    private final Duration refreshTokenDuration;

    private final JWSSigner signer;

    private final JWSVerifier verifier;

    private final JWSAlgorithm algorithm;

    private final String password;

    public TokenProvider(PublicKey publicKey, PrivateKey privateKey, Duration accessTokenDuration, Duration refreshTokenDuration, String password) {
        this.password = password;
        JWSSigner initSigner;
        JWSVerifier initVerifier;
        JWSAlgorithm initAlgorithm;

        if (publicKey != null) {
            if (publicKey instanceof RSAPublicKey rsaPublicKey) {
                initVerifier = new RSASSAVerifier(rsaPublicKey);
            } else if (publicKey instanceof ECPublicKey ecPublicKey) {
                try {
                    initVerifier = new ECDSAVerifier(ecPublicKey);
                } catch (JOSEException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalArgumentException("Unsupported PublicKey type: expected RSAPublicKey or ECPublicKey.");
            }
        } else {
            try {
                initVerifier = new MACVerifier("siyukio-siyukio-siyukio-siyukio!");
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
        }

        if (privateKey != null) {
            if (privateKey instanceof RSAPrivateKey rsaPrivateKey) {
                initSigner = new RSASSASigner(rsaPrivateKey);
                initAlgorithm = JWSAlgorithm.RS256;
            } else if (privateKey instanceof ECPrivateKey ecPrivateKey) {
                try {
                    initSigner = new ECDSASigner(ecPrivateKey);
                } catch (JOSEException e) {
                    throw new RuntimeException(e);
                }
                initAlgorithm = JWSAlgorithm.ES256;
            } else {
                throw new IllegalArgumentException("Unsupported PrivateKey type: expected RSAPrivateKey or ECPrivateKey.");
            }
        } else {
            try {
                initSigner = new MACSigner("siyukio-siyukio-siyukio-siyukio!");
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
            initAlgorithm = JWSAlgorithm.HS256;
        }

        this.signer = initSigner;
        this.verifier = initVerifier;
        this.algorithm = initAlgorithm;
        this.accessTokenDuration = accessTokenDuration;
        this.refreshTokenDuration = refreshTokenDuration;
    }

    public String createAuthorization(Token token) {
        return this.createAuthorization(token, null);
    }

    public String createAuthorization(Token token, Duration duration) {
        if (duration == null) {
            duration = token.type() != null && token.type().equals(Token.Type.REFRESH) ? this.refreshTokenDuration : this.accessTokenDuration;
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(duration.toMillis());

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
        JSONObject tokenJson = XDataUtils.copy(token, JSONObject.class);
        tokenJson.toMap().forEach((k, v) -> {
            if (v != null) {
                builder.claim(k, v);
            }
        });
        JWTClaimsSet claims = builder.jwtID(IdUtils.getUniqueId())
                .expirationTime(new Date(expiresAt.toEpochMilli()))
                .build();
        JWSHeader header = new JWSHeader.Builder(this.algorithm)
                .build();

        String plainText = claims.toPayload().toString();
        String payload;
        if (StringUtils.hasText(this.password)) {
            payload = CryptoUtils.encrypt(this.password, plainText);
        } else {
            payload = plainText;
        }

        JWSObject jwsObject = new JWSObject(header, new Payload(payload));

        try {
            jwsObject.sign(this.signer);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }

    public Token verifyToken(String authorization) {
        try {
            JWSObject jwsObject = JWSObject.parse(authorization);
            // validate signature
            boolean valid = this.verifier.verify(jwsObject.getHeader(), jwsObject.getSigningInput(), jwsObject.getSignature());
            if (!valid) {
                return null;
            }

            String cipherText = jwsObject.getPayload().toString();
            String plainText;
            if (StringUtils.hasText(this.password)) {
                plainText = CryptoUtils.decrypt(this.password, cipherText);
            } else {
                plainText = cipherText;
            }
            JWTClaimsSet claims = JWTClaimsSet.parse(plainText);

            // validate expiration time
            Date expirationDate = claims.getExpirationTime();
            boolean expired = Instant.now().toEpochMilli() > expirationDate.getTime();
            if (expired) {
                return null;
            }
            return XDataUtils.copy(claims.getClaims(), Token.class);
        } catch (Exception e) {
            return null;
        }
    }

}
