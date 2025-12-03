package io.github.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author Buddy
 */
@Slf4j
public abstract class CryptoUtils {

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
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            // Generate 12-byte IV for GCM
            byte[] iv = new byte[12];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext (IV || ciphertextWithTag)
            byte[] out = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherBytes, 0, out, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(out);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
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
            byte[] all = Base64.getDecoder().decode(base64CipherText);
            if (all.length < 12) {
                throw new IllegalArgumentException("Invalid cipher text: too short to contain IV");
            }

            // Extract IV and ciphertext
            byte[] iv = new byte[12];
            System.arraycopy(all, 0, iv, 0, 12);
            byte[] cipherBytes = new byte[all.length - 12];
            System.arraycopy(all, 12, cipherBytes, 0, cipherBytes.length);

            // Derive key
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(password.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
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
}
