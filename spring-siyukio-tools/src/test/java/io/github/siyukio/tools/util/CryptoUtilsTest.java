package io.github.siyukio.tools.util;

import org.junit.jupiter.api.Test;

/**
 * @author Buddy
 */
public class CryptoUtilsTest {

    @Test
    void testCreateRSAKeyPair() throws Exception {
        CryptoUtils.createRSAKeyPair();
    }

    @Test
    void testCreateECKeyPair() throws Exception {
        CryptoUtils.createECKeyPair();
    }
}
