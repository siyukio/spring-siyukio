package io.github.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Buddy
 */
@Slf4j
public abstract class CryptoUtils {

    private static final GcmIvGenerator GCM_IV_GENERATOR = new GcmIvGenerator();

    private static final ThreadLocal<Cipher> TL_AES_GCM_ENCRYPT =
            ThreadLocal.withInitial(() -> {
                try {
                    return Cipher.getInstance("AES/GCM/NoPadding");
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
            });

    public static PrivateKey getPrivateKeyFromPem(String pem) throws Exception {
        String privateKeyPEM = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", ""); //

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        try {
            KeyFactory kf = KeyFactory.getInstance("EC");
            PrivateKey privateKey = kf.generatePrivate(keySpec);
            log.info("Find ECDSA private key");
            return privateKey;
        } catch (Exception ex) {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(keySpec);
            log.info("Find RSA private key");
            return privateKey;
        }
    }

    public static PublicKey getPublicKeyFromPem(String pem) throws Exception {
        String publicKeyPEM = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");  // 去掉空格和换行

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PublicKey key = keyFactory.generatePublic(keySpec);
            log.info("Find ECDSA public key");
            return key;
        } catch (Exception ex) {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey key = keyFactory.generatePublic(keySpec);
            log.info("Find RSA public key");
            return key;
        }
    }

    public static String toPem(String title, byte[] bytes) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(bytes);
        return "-----BEGIN " + title + "-----\n" +
                encoded + "\n" +
                "-----END " + title + "-----";
    }

    public static String publicKeyToPem(PublicKey publicKey) {
        return toPem("PUBLIC KEY", publicKey.getEncoded()); // X.509 格式
    }

    public static String privateKeyToPem(PrivateKey privateKey) {
        return toPem("PRIVATE KEY", privateKey.getEncoded()); // PKCS#8 格式
    }

    public static void createRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        String publicPem = publicKeyToPem(pair.getPublic());
        String privatePem = privateKeyToPem(pair.getPrivate());

        log.info("RSA Public Key:\n{}", publicPem);
        log.info("RSA Private Key:\n{}", privatePem);
    }

    public static void createECKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        KeyPair pair = keyGen.generateKeyPair();

        String publicPem = publicKeyToPem(pair.getPublic());
        String privatePem = privateKeyToPem(pair.getPrivate());

        log.info("EC Public Key:\n{}", publicPem);
        log.info("EC Private Key:\n{}", privatePem);
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString(); // returns a 32-character lowercase hexadecimal MD5 string.
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found");
        }
    }

    /**
     * Encrypt a plaintext string using AES-GCM with a key derived from the provided password.
     * <p>
     * Key derivation: SHA-256(password) -> 32-byte AES key (AES-256).
     * IV: randomly generated 12-byte nonce (recommended for GCM).
     * Output format: Base64( iv || ciphertextWithAuthTag )
     *
     * @param password  password to derive AES key from (UTF-8)
     * @param plaintext the plaintext to encrypt (UTF-8)
     * @return base64 encoded string containing the iv followed by ciphertext+tag
     * @throws RuntimeException on encryption errors
     */
    public static String encrypt(String password, String plaintext) {
        try {
            // Derive 256-bit key from password using SHA-256
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(password.getBytes(StandardCharsets.UTF_8));
            return encrypt(keyBytes, plaintext);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    /**
     * Decrypt a base64-encoded (iv || ciphertextWithTag) produced by {@link #encrypt(String, String)}.
     *
     * @param password         password used to derive the AES key (UTF-8)
     * @param base64CipherText base64 string containing iv followed by ciphertext+tag
     * @return decrypted plaintext (UTF-8)
     * @throws IllegalArgumentException if input is invalid
     * @throws RuntimeException         on decryption/authentication failure
     */
    public static String decrypt(String password, String base64CipherText) {
        try {
            // Derive key
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(password.getBytes(StandardCharsets.UTF_8));
            return decrypt(keyBytes, base64CipherText);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    /**
     * Generate a random 256-bit master key for encryption.
     * <p>
     * Uses cryptographically secure random number generator to create
     * a Base64-encoded key suitable for HMAC-SHA256 key derivation.
     *
     * @return Base64-encoded 256-bit random key
     */
    public static String randomMasterKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32]; // 256 bit
        secureRandom.nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    /**
     * Generate a random 128-bit salt for key derivation.
     * <p>
     * Uses cryptographically secure random number generator to create
     * a Base64-encoded salt suitable for HMAC-SHA256 key derivation.
     *
     * @return Base64-encoded 128-bit random salt
     */
    public static String randomSalt() {
        byte[] randomBytes = GCM_IV_GENERATOR.nextIv();
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    /**
     * Encrypt plaintext using AES-GCM with provided key bytes.
     * <p>
     * Uses the provided AES key directly without any derivation.
     * IV: randomly generated 12-byte nonce (recommended for GCM).
     * Output format: Base64( iv || ciphertextWithAuthTag )
     *
     * @param keyBytes  AES key bytes (16, 24, or 32 bytes for AES-128/192/256)
     * @param plaintext plaintext to encrypt (UTF-8)
     * @return Base64-encoded ciphertext (iv || ciphertext+tag)
     * @throws IllegalArgumentException if key length is invalid
     * @throws RuntimeException         on encryption errors
     */
    public static String encrypt(byte[] keyBytes, String plaintext) {
        if (keyBytes == null || keyBytes.length < 16 || keyBytes.length > 32) {
            throw new IllegalArgumentException("AES key must be 16, 24, or 32 bytes");
        }

        try {
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            // Generate 12-byte IV for GCM
            byte[] iv = GCM_IV_GENERATOR.nextIv();

            Cipher cipher = TL_AES_GCM_ENCRYPT.get();
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext (IV || ciphertextWithTag)
            byte[] out = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherBytes, 0, out, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(out);
        } catch (InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    /**
     * Decrypt ciphertext using AES-GCM with provided key bytes.
     * <p>
     * Uses the provided AES key directly without any derivation.
     * Input format: Base64( iv || ciphertextWithAuthTag )
     *
     * @param keyBytes         AES key bytes (16, 24, or 32 bytes for AES-128/192/256)
     * @param base64CipherText Base64-encoded ciphertext (iv || ciphertext+tag)
     * @return decrypted plaintext (UTF-8)
     * @throws IllegalArgumentException if key length is invalid or input is invalid
     * @throws RuntimeException         on decryption/authentication failure
     */
    public static String decrypt(byte[] keyBytes, String base64CipherText) {
        if (keyBytes == null || keyBytes.length < 16 || keyBytes.length > 32) {
            throw new IllegalArgumentException("AES key must be 16, 24, or 32 bytes");
        }

        try {
            byte[] all = Base64.getDecoder().decode(base64CipherText);
            if (all.length < 12) {
                throw new IllegalArgumentException("Invalid cipher text: too short to contain IV");
            }

            // Extract IV and ciphertext
            byte[] iv = new byte[12];
            System.arraycopy(all, 0, iv, 0, 12);
            byte[] cipherBytes = new byte[all.length - 12];
            System.arraycopy(all, 12, cipherBytes, 0, cipherBytes.length);

            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = TL_AES_GCM_ENCRYPT.get();
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }

    /**
     * Derive a 256-bit encryption key using HMAC-SHA256.
     * <p>
     * Uses master key as HMAC key and salt combined with info as message
     * to generate a cryptographically strong derived key. The info parameter
     * provides context for the key derivation, enabling different keys for
     * different purposes (e.g., encryption vs. authentication).
     *
     * @param masterKey Base64-encoded master key (256 bits)
     * @param salt      Base64-encoded salt (any length)
     * @param info      Contextual information for key derivation (UTF-8)
     * @return 32-byte AES-256 key
     * @throws IllegalArgumentException if keys are invalid
     */
    public static byte[] deriveKey(String masterKey, String salt, String info) {
        try {
            byte[] masterKeyBytes = Base64.getDecoder().decode(masterKey);
            byte[] saltBytes = Base64.getDecoder().decode(salt);

            if (masterKeyBytes.length < 16) {
                throw new IllegalArgumentException("Master key must be at least 128 bits");
            }

            // Combine salt and info for context-specific key derivation
            byte[] saltWithInfo = saltBytes;
            if (info != null && !info.isEmpty()) {
                byte[] infoBytes = info.getBytes(StandardCharsets.UTF_8);
                saltWithInfo = new byte[saltBytes.length + infoBytes.length];
                System.arraycopy(saltBytes, 0, saltWithInfo, 0, saltBytes.length);
                System.arraycopy(infoBytes, 0, saltWithInfo, saltBytes.length, infoBytes.length);
            }

            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec hmacKey = new SecretKeySpec(masterKeyBytes, "HmacSHA256");
            hmac.init(hmacKey);

            return hmac.doFinal(saltWithInfo);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 key derivation failed", e);
        }
    }

    /**
     * Encrypt plaintext using HMAC-SHA256 derived key with context info.
     * <p>
     * Derives the encryption key from master key, salt, and info using HMAC-SHA256,
     * then encrypts the plaintext using AES-GCM.
     *
     * @param masterKey Base64-encoded master key (256 bits)
     * @param salt      Base64-encoded salt
     * @param info      Contextual information for key derivation (UTF-8)
     * @param plaintext plaintext to encrypt (UTF-8)
     * @return Base64-encoded ciphertext (iv || ciphertext+tag)
     * @throws RuntimeException on encryption errors
     */
    public static String encrypt(String masterKey, String salt, String info, String plaintext) {
        byte[] keyBytes = deriveKey(masterKey, salt, info);
        return encrypt(keyBytes, plaintext);
    }

    /**
     * Decrypt ciphertext using HMAC-SHA256 derived key with context info.
     * <p>
     * Derives the encryption key from master key, salt, and info using HMAC-SHA256,
     * then decrypts the AES-GCM ciphertext.
     *
     * @param masterKey        Base64-encoded master key (256 bits)
     * @param salt             Base64-encoded salt
     * @param info             Contextual information for key derivation (UTF-8)
     * @param base64CipherText Base64-encoded ciphertext (iv || ciphertext+tag)
     * @return decrypted plaintext (UTF-8)
     * @throws IllegalArgumentException if input is invalid
     * @throws RuntimeException         on decryption/authentication failure
     */
    public static String decrypt(String masterKey, String salt, String info, String base64CipherText) {
        byte[] keyBytes = deriveKey(masterKey, salt, info);
        return decrypt(keyBytes, base64CipherText);
    }

    public static class GcmIvGenerator {
        private static final int THRESHOLD = Integer.MAX_VALUE - 10000000;
        private final AtomicInteger counter = new AtomicInteger();
        private final AtomicReference<byte[]> prefixRef = new AtomicReference<>(generateNewPrefix());

        public GcmIvGenerator() {
            counter.set(generateRandomCounterStart());
        }

        private static byte[] generateNewPrefix() {
            byte[] p = new byte[8];
            new SecureRandom().nextBytes(p);
            return p;
        }

        /**
         * Generate a random starting counter value within a reasonable range.
         * <p>
         * Divides int range into segments and picks a random start position
         * to avoid starting from 0 every time, reducing the risk of IV collision
         * when the prefix is updated.
         *
         * @return random counter value between 0 and THRESHOLD
         */
        private static int generateRandomCounterStart() {
            return new SecureRandom().nextInt(THRESHOLD);
        }

        public byte[] nextIv() {
            int count = counter.getAndIncrement();

            // check need to generate new prefix
            if (count > THRESHOLD) {
                // try to update prefix
                byte[] oldPrefix = prefixRef.get();
                byte[] newPrefix = generateNewPrefix();
                if (prefixRef.compareAndSet(oldPrefix, newPrefix)) {
                    counter.set(generateRandomCounterStart());  // reset counter with random start
                    count = counter.get();
                } else {
                    // other thread has updated prefix, retry
                    count = counter.get();
                }
            }

            byte[] prefix = prefixRef.get();
            byte[] iv = new byte[12];
            System.arraycopy(prefix, 0, iv, 0, 8);
            iv[8] = (byte) (count >>> 24);
            iv[9] = (byte) (count >>> 16);
            iv[10] = (byte) (count >>> 8);
            iv[11] = (byte) (count);
            return iv;
        }
    }
}
