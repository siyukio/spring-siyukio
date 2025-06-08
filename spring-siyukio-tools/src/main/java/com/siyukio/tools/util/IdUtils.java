package com.siyukio.tools.util;

import com.github.f4b6a3.uuid.alt.GUID;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author Bugee
 */
public final class IdUtils {

    private static final char[] BASE = (
            "0123456789"
                    + "ABCDEFGHJK"
                    + "LMNPQRSTVW"
                    + "XYZ"
                    + "abcdefghjk"
                    + "mnpqrstvwx"
                    + "yz").toCharArray();

    public static String getUniqueId() {
        String id = GUID.v7().toUUID().toString().replaceAll("-", "");
        return toBase(id);
    }

    public static String toBase(String id) {
        BigInteger big = new BigInteger(id, 16);
        StringBuilder sb = new StringBuilder();
        while (big.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divMod = big.divideAndRemainder(BigInteger.valueOf(BASE.length));
            sb.append(BASE[divMod[1].intValue()]);
            big = divMod[0];
        }
        return sb.reverse().toString();
    }

    public static String fromBase(String base) {
        BigInteger value = BigInteger.ZERO;
        for (char c : base.toCharArray()) {
            value = value.multiply(BigInteger.valueOf(BASE.length)).add(BigInteger.valueOf(Arrays.binarySearch(BASE, c)));
        }
        return value.toString(16);
    }

}
