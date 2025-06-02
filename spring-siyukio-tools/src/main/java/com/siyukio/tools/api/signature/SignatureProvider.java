package com.siyukio.tools.api.signature;

import com.siyukio.tools.api.ApiException;
import com.siyukio.tools.collection.ConcurrentCache;
import com.siyukio.tools.util.MessageDigestUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * @author Buddy
 */
public class SignatureProvider {

    private final ConcurrentCache<String, String> cache = new ConcurrentCache<>(10000);

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
        String value = this.cache.get(nonce);
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
        return MessageDigestUtils.md5(text);
    }
}
