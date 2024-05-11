package cashu.util;

import lombok.NonNull;
import nostr.base.PrivateKey;
import nostr.id.Identity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Utils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toLowerCase().toCharArray();


    public static byte[] getPublicKey(byte[] privateKey) {
        return Identity.create(new PrivateKey(privateKey)).getPublicKey().getRawData();
    }

    public static byte[] generatePrivateKey() {
        return PrivateKey.generateRandomPrivKey().getRawData();
    }

    public static String bytesToHex(@NonNull byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(@NonNull String s) {
        int len = s.length();
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
}
