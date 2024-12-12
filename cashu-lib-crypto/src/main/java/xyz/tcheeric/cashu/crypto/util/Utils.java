package xyz.tcheeric.cashu.crypto.util;

import lombok.NonNull;

import java.math.BigInteger;
import java.util.Arrays;

public class Utils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toLowerCase().toCharArray();

    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToBytes(@NonNull String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length");
        }

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int digit1 = Character.digit(s.charAt(i), 16);
            int digit2 = Character.digit(s.charAt(i + 1), 16);

            if (digit1 == -1 || digit2 == -1) {
                throw new IllegalArgumentException("Invalid hexadecimal character found");
            }

            data[i / 2] = (byte) ((digit1 << 4) + digit2);
        }
        return data;
    }

    public static byte[] bytesFromBigInteger(@NonNull BigInteger n) {
        byte[] b = n.toByteArray();
        if (b.length == 32) {
            return b;
        } else if (b.length > 32) {
            return Arrays.copyOfRange(b, b.length - 32, b.length);
        } else {
            byte[] buf = new byte[32];
            System.arraycopy(b, 0, buf, buf.length - b.length, b.length);
            return buf;
        }
    }

    public static BigInteger bigIntFromBytes(byte[] b) {
        return new BigInteger(1, b);
    }

}
