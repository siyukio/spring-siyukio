package io.github.siyukio.tools.api.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.github.siyukio.tools.util.JsonUtils;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * @author Buddy
 */
public final class TokenProvider {

    private final Duration accessTokenDuration;

    private final Duration refreshTokenDuration;

    private final Algorithm algorithm;

    public TokenProvider(RSAPublicKey publicKey, RSAPrivateKey privateKey, Duration accessTokenDuration, Duration refreshTokenDuration) {
        if (publicKey == null && privateKey == null) {
            this.algorithm = Algorithm.HMAC256("siyukio");
        } else {
            this.algorithm = Algorithm.RSA256(publicKey, privateKey);
        }
        this.accessTokenDuration = accessTokenDuration;
        this.refreshTokenDuration = refreshTokenDuration;
    }

    public String createAuthorization(String uid, String name, boolean refresh) {
        Token token = Token.builder().uid(uid).name(name).refresh(refresh).build();
        return this.createAuthorization(token);
    }

    public String createAuthorization(List<String> roles, boolean refresh) {
        Token token = Token.builder().roles(roles).refresh(refresh).build();
        return this.createAuthorization(token);
    }

    public String createAuthorization(Token token) {
        Duration duration = token.refresh ? this.refreshTokenDuration : this.accessTokenDuration;
        Date expireTime = new Date(System.currentTimeMillis() + duration.toMillis());
        String json = JsonUtils.toJSONString(token);
        return JWT.create()
                .withSubject(json)
                .withExpiresAt(expireTime)
                .sign(this.algorithm);
    }

    public Token verifyToken(String authorization) {
        Token token;
        try {
            JWTVerifier verifier = JWT.require(this.algorithm).build();
            DecodedJWT jwt = verifier.verify(authorization);
            String json = jwt.getSubject();
            token = JsonUtils.parse(json, Token.class);
        } catch (TokenExpiredException e) {
            token = Token.builder().expired(true).build();
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
            token = JsonUtils.parse(json, Token.class);
        } catch (JWTDecodeException e) {
            token = null;
        }
        return token;
    }


}
