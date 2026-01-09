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
}
