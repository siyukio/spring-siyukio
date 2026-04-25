package io.github.siyukio.tools.api.signature;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.util.CryptoUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author Buddy
 */
public class SignatureProvider {

    private final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .softValues()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private final String salt;

    public SignatureProvider(String salt) {
        this.salt = salt;
    }

    public void validate(long timestamp, String nonce, String signature) {
        if (!StringUtils.hasText(this.salt)) {
            return;
        }
        if (timestamp == 0) {
            throw ApiException.getInvalidApiException("signature error: miss timestamp");
        }
        if (!StringUtils.hasText(nonce)) {
            throw ApiException.getInvalidApiException("signature error: miss nonce");
        }
        if (!StringUtils.hasText(signature)) {
            throw ApiException.getInvalidApiException("signature error: miss signature");
        }
        if (timestamp < System.currentTimeMillis() - Duration.ofDays(1).toMillis()) {
            throw ApiException.getInvalidApiException("signature error: timestamp expired");
        }
        String value = this.cache.getIfPresent(nonce);
        if (value != null) {
            throw ApiException.getInvalidApiException("signature error: nonce used");
        }
        this.cache.put(nonce, "");

        String mySignature = this.createSignature(timestamp, nonce);
        if (!mySignature.equals(signature)) {
            throw ApiException.getInvalidApiException("signature error");
        }
    }

    public String createSignature(long timestamp, String nonce) {
        String text = this.salt + timestamp + nonce;
        return CryptoUtils.md5(text);
    }
}
