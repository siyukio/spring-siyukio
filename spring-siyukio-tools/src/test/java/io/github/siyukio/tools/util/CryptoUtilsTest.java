package io.github.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * @author Buddy
 */

@Slf4j
public class CryptoUtilsTest {

    @Test
    void testCreateRSAKeyPair() throws Exception {
        CryptoUtils.createRSAKeyPair();
    }

    @Test
    void testCreateECKeyPair() throws Exception {
        CryptoUtils.createECKeyPair();
    }

    @Test
    void testCreateMasterKey() throws Exception {
        String masterKey = CryptoUtils.randomMasterKey();
        log.info("masterKey: {}", masterKey);
    }

    @Test
    void testEncryptSha256() throws Exception {
        String password = CryptoUtils.randomMasterKey();
        String encryptedText = CryptoUtils.encrypt(password, "hello");
        String decryptedText = CryptoUtils.decrypt(password, encryptedText);
        log.info("sha256: {}, {}", encryptedText, decryptedText);
    }

    @Test
    void testEncryptHmacSHA256() throws Exception {
        String masterKey = CryptoUtils.randomMasterKey();
        String salt = CryptoUtils.randomSalt();
        String encryptedText = CryptoUtils.encrypt(masterKey, salt, "test", "hello");
        String decryptedText = CryptoUtils.decrypt(masterKey, salt, "test", encryptedText);
        log.info("HmacSHA256: {}, {}", encryptedText, decryptedText);
    }

    @Test
    void testEncryptHmacSHA256Batch() throws Exception {
        String masterKey = CryptoUtils.randomMasterKey();
        String salt = CryptoUtils.randomSalt();
        for (int i = 0; i < 1000; i++) {
            String encryptedText = CryptoUtils.encrypt(masterKey, salt, "test", "hello");
            log.info("encryptedText: {}", encryptedText);
        }
    }

    @Test
    void testHmacSha256() {
        String secret = CryptoUtils.randomMasterKey();
        log.info("secret: {}", secret);
        String input = "gi9nm8iCHkwfCBBSyt8SmZ_g6SJqepZkWMEsjdYhycULk";
        String result = CryptoUtils.hmacSha256(secret, input);
        log.info("hmacSha256: {}", result);

        // 84c9867b45e44b1b542c5954b5277b6bf927ed1edf0c8ee81ca6f36797624163
        // Verify deterministic output: same input + secret = same result
        String result2 = CryptoUtils.hmacSha256(secret, input);
        assert result.equals(result2);
    }
}
