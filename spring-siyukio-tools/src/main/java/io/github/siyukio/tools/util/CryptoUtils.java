package io.github.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author Buddy
 */
@Slf4j
public abstract class CryptoUtils {

    public static RSAPrivateKey getPrivateKeyFromPem(String pem) throws Exception {
        String privateKeyPEM = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", ""); //

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(keySpec);

        return (RSAPrivateKey) privateKey;
    }

    public static RSAPublicKey getPublicKeyFromPem(String pem) throws Exception {
        String publicKeyPEM = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");  // 去掉空格和换行

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        PublicKey key = keyFactory.generatePublic(keySpec);

        return (RSAPublicKey) key;
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

    public static void createKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        String publicPem = publicKeyToPem(pair.getPublic());
        String privatePem = privateKeyToPem(pair.getPrivate());

        log.info("Public Key:\n{}", publicPem);
        log.info("Private Key:\n{}", privatePem);
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
}
