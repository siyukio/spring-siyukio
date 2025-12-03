package io.github.siyukio.tools.api.token;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.JWTClaimsSet;
import io.github.siyukio.tools.util.CryptoUtils;
import io.github.siyukio.tools.util.IdUtils;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

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
                initVerifier = new MACVerifier("siyukio");
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
                initSigner = new MACSigner("siyukio");
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
        Duration duration = token.refresh() ? this.refreshTokenDuration : this.accessTokenDuration;
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(duration.toMillis());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(IdUtils.getUniqueId())
                .subject(token.uid())
                .claim("e", token.name())
                .claim("s", token.roles())
                .claim("h", token.refresh())
//                .issueTime(new Date(issuedAt.toEpochMilli()))
                .expirationTime(new Date(expiresAt.toEpochMilli()))
                .build();
        JWSHeader header = new JWSHeader.Builder(this.algorithm)
                .type(JOSEObjectType.JWT)
                .build();

        String plainText = claims.toPayload().toString();
        String cipherText = CryptoUtils.encrypt(this.password, plainText);
        Payload payload = new Payload(cipherText);

        JWSObject jwsObject = new JWSObject(header, payload);

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
            String plainText = CryptoUtils.decrypt(this.password, cipherText);
            JWTClaimsSet claims = JWTClaimsSet.parse(plainText);

            // validate expiration time
            Date expirationDate = claims.getExpirationTime();
            boolean expired = Instant.now().toEpochMilli() > expirationDate.getTime();
            if (expired) {
                return null;
            }

            String id = claims.getJWTID();
            String uid = claims.getSubject();
            String name = claims.getStringClaim("e");
            List<String> roles = claims.getStringListClaim("s");
            boolean refresh = claims.getBooleanClaim("h");

            return Token.builder()
                    .id(id).uid(uid).name(name).roles(roles).refresh(refresh)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

}
