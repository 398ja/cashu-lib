package xyz.tcheeric.cashu.crypto.util;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

@AllArgsConstructor
public class KeySetDerivation {

    public static String getId(@NonNull Map<BigInteger, byte[]> keys) {
        Map<BigInteger, byte[]> sortedKeys = new TreeMap<>(keys);

        try (ByteArrayOutputStream pubkeysConcat = new ByteArrayOutputStream()) {
            for (byte[] publicKey : sortedKeys.values()) {
                pubkeysConcat.write(publicKey);
            }

            byte[] hash = Utils.sha256(pubkeysConcat.toByteArray());
            String pkHash = Utils.bytesToHexString(hash);

            return "00" + pkHash.substring(0, 14);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
