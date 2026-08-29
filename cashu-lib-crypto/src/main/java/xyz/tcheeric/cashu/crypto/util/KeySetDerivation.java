package xyz.tcheeric.cashu.crypto.util;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Derives a NUT-02 keyset id.
 *
 * <p>{@link #getId(Map)} is the version 1 derivation over the public keys alone. New keysets
 * should be derived through {@link KeySetIdV2Derivation}, which commits to the keyset's unit, fee
 * and expiry as well, so a changed fee is a different keyset rather than the same one behaving
 * differently.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/02.md">NUT-02</a>
 */
@AllArgsConstructor
public class KeySetDerivation {

    public static String getId(@NonNull Map<BigInteger, byte[]> keys) {
        Map<BigInteger, byte[]> sortedKeys = new TreeMap<>(keys);

        try (ByteArrayOutputStream pubkeysConcat = new ByteArrayOutputStream()) {
            for (byte[] publicKey : sortedKeys.values()) {
                // Concatenate compressed public key bytes per NUT-02
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
