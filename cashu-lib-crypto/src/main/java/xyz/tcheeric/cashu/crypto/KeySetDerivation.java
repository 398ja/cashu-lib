package xyz.tcheeric.cashu.crypto;

import xyz.tcheeric.cashu.common.model.KeySet;
import xyz.tcheeric.cashu.common.model.Keys;
import xyz.tcheeric.cashu.common.model.PublicKey;
import xyz.tcheeric.cashu.crypto.util.Utils;
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

    public void deriveKeySetId() {
        var ksId = deriveKeySetId(keySet.getKeys());
        this.keySet.setId(ksId);
    }

    public static String deriveKeySetId(@NonNull Keys keys) {
        Map<BigInteger, PublicKey> sortedKeys = new TreeMap<>(keys.getValues());

        try (ByteArrayOutputStream pubkeysConcat = new ByteArrayOutputStream()) {
            for (PublicKey publicKey : sortedKeys.values()) {
                pubkeysConcat.write(publicKey.toBytes());
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pubkeysConcat.toByteArray());

            String pkHash = Utils.bytesToHexString(hash);
            return "00" + pkHash.substring(0, 14);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
