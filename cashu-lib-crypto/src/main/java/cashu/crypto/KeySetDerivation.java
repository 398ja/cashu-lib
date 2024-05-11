package cashu.crypto;

import cashu.common.model.Hex;
import cashu.common.model.KeySet;
import cashu.common.model.Keys;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

@AllArgsConstructor
public class KeySetDerivation {

    private final KeySet keySet;

    public String deriveKeySetId() {
        return deriveKeySetId(keySet.getKeys());
    }

    public static String deriveKeySetId(@NonNull Keys keys) {
        Map<BigInteger, Hex> sortedKeys = new TreeMap<>(keys.getValues());

        try (ByteArrayOutputStream pubkeysConcat = new ByteArrayOutputStream()) {
            for (Hex hex : sortedKeys.values()) {
                pubkeysConcat.write(hex.getBytes());
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pubkeysConcat.toByteArray());

            Hex hexHash = Hex.fromBytes(hash);
            return "00" + hexHash.toString().substring(0, 14);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
