package io.github.siyukio.tools.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Buddy
 */
public abstract class MessageDigestUtils {

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
