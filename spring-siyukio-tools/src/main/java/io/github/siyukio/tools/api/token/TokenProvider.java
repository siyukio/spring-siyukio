package io.github.siyukio.tools.api.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * @author Buddy
 */
@Slf4j
public final class TokenProvider {

    private final Duration accessTokenDuration;

    private final Duration refreshTokenDuration;

    private final Algorithm algorithm;

    public TokenProvider(PublicKey publicKey, PrivateKey privateKey, Duration accessTokenDuration, Duration refreshTokenDuration) {
        if (publicKey != null && privateKey != null) {
            if (publicKey instanceof RSAPublicKey rsaPublicKey && privateKey instanceof RSAPrivateKey rsaPrivateKey) {
                this.algorithm = Algorithm.RSA256(rsaPublicKey, rsaPrivateKey);
            } else if (publicKey instanceof ECPublicKey ecPublicKey && privateKey instanceof ECPrivateKey ecPrivateKey) {
                this.algorithm = Algorithm.ECDSA256(ecPublicKey, ecPrivateKey);
            } else {
                throw new IllegalArgumentException("PublicKey and PrivateKey type mismatch: expected both RSA or both EC.");
            }
        } else if (publicKey != null) {
            if (publicKey instanceof RSAPublicKey rsaPublicKey) {
                this.algorithm = Algorithm.RSA256(rsaPublicKey, null);
            } else if (publicKey instanceof ECPublicKey ecPublicKey) {
                this.algorithm = Algorithm.ECDSA256(ecPublicKey, null);
            } else {
                throw new IllegalArgumentException("Unsupported PublicKey type: expected RSAPublicKey or ECPublicKey.");
            }
        } else if (privateKey != null) {
            if (privateKey instanceof RSAPrivateKey rsaPrivateKey) {
                this.algorithm = Algorithm.RSA256(null, rsaPrivateKey);
            } else if (privateKey instanceof ECPrivateKey ecPrivateKey) {
                this.algorithm = Algorithm.ECDSA256(null, ecPrivateKey);
            } else {
                throw new IllegalArgumentException("Unsupported PrivateKey type: expected RSAPrivateKey or ECPrivateKey.");
            }
        } else {
            this.algorithm = Algorithm.HMAC256("siyukio");
        }
        this.accessTokenDuration = accessTokenDuration;
        this.refreshTokenDuration = refreshTokenDuration;
    }

    public String createAuthorization(Token token) {
        Duration duration = token.refresh() ? this.refreshTokenDuration : this.accessTokenDuration;
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(duration.toMillis());
        return JWT.create()
                .withJWTId(IdUtils.getUniqueId())
                .withSubject(token.uid())
                .withClaim("n", token.name())
                .withClaim("r", token.roles())
                .withClaim("a", !token.refresh())
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(this.algorithm);
    }

    public Token verifyToken(String authorization) {
        Token token;
        try {
            JWTVerifier verifier = JWT.require(this.algorithm).build();
            DecodedJWT jwt = verifier.verify(authorization);
            String id = jwt.getId();
            String uid = jwt.getSubject();
            String name = jwt.getClaim("n").asString();
            List<String> roles = jwt.getClaim("r").asList(String.class);
            boolean accessToken = jwt.getClaim("a").asBoolean();
            token = Token.builder()
                    .id(id).uid(uid).name(name).roles(roles).refresh(!accessToken)
                    .build();
        } catch (TokenExpiredException e) {
            Token expiredToken = this.decode(authorization);
            if (expiredToken != null) {
                log.warn("token expired: {}, {}, {}", expiredToken.id(), expiredToken.uid(), expiredToken.name());
            }
            token = null;
        } catch (JWTVerificationException e) {
            token = null;
        }
        return token;
    }

    /**
     * 解码auth
     *
     * @return
     */
    public Token decode(String authorization) {
        Token token;
        try {
            DecodedJWT jwt = JWT.decode(authorization);
            String json = jwt.getSubject();
            token = XDataUtils.parse(json, Token.class);
        } catch (JWTDecodeException e) {
            token = null;
        }
        return token;
    }


}
