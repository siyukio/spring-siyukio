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

    private final boolean skip;

    public SignatureProvider(String salt, boolean skip) {
        this.salt = salt;
        this.skip = skip;
    }

    public boolean validate(String ts, String nonce, String sign) {
        if (this.skip) {
            return true;
        }
        if (!StringUtils.hasText(ts)) {
            throw ApiException.getInvalidApiException("sign error: miss ts");
        }
        if (!StringUtils.hasText(nonce)) {
            throw ApiException.getInvalidApiException("sign error: miss nonce");
        }
        if (!StringUtils.hasText(sign)) {
            throw ApiException.getInvalidApiException("sign error: miss sign");
        }
        try {
            long tsTime = Long.parseLong(ts);
            if (tsTime < System.currentTimeMillis() - Duration.ofDays(1).toMillis()) {
                throw ApiException.getInvalidApiException("sign error: ts expired");
            }
        } catch (NumberFormatException e) {
            throw ApiException.getInvalidApiException("sign error: ts type error");
        }
        String value = this.cache.get(nonce);
        if (value != null) {
            throw ApiException.getInvalidApiException("sign error: nonce error");
        }
        this.cache.put(nonce, "");

        String text = this.salt + ts + nonce;
        String mySign = MessageDigestUtils.md5(text);
        if (!mySign.equals(sign)) {
            throw ApiException.getInvalidApiException("sign error: md5 error");
        }
        return true;
    }
}
