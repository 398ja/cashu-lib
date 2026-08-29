package xyz.tcheeric.cashu.crypto.util;

import lombok.NonNull;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * Derives a NUT-02 version 2 keyset id.
 *
 * <p>Unlike version 1, which hashes the public keys alone, a v2 id commits to the keyset's
 * <em>metadata</em> as well: the unit, the input fee and the final expiry. That is the point of the
 * version. Under v1 a mint could change {@code input_fee_ppk} while keeping the same keyset id, so
 * a wallet holding a proof had no way to tell which fee it had agreed to; under v2 a changed fee is
 * a different keyset by construction.
 *
 * <p>The preimage is a text format, so every detail of it matters. Amounts are decimal, keys are
 * lowercase hex, pairs are comma-separated, and the metadata is appended as {@code |key:value}
 * segments in a fixed order.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/02.md">NUT-02</a>
 */
public final class KeySetIdV2Derivation {

    /** The version byte every v2 id carries, as it appears in the hex id. */
    private static final String VERSION_BYTE = "01";

    private KeySetIdV2Derivation() {
    }

    /**
     * Derives the id of a keyset from its keys and metadata.
     *
     * @param keys         amount to compressed public key, in any order; sorted here
     * @param unit         the keyset's unit, e.g. {@code sat}
     * @param inputFeePpk  the input fee, or null; zero and null are both omitted from the preimage
     * @param finalExpiry  the final expiry as a unix timestamp, or null
     */
    public static String getId(@NonNull Map<BigInteger, byte[]> keys,
                               @NonNull String unit,
                               Integer inputFeePpk,
                               Long finalExpiry) {
        try {
            byte[] hash = Utils.sha256(preimage(keys, unit, inputFeePpk, finalExpiry));
            return VERSION_BYTE + Utils.bytesToHexString(hash).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required to derive a keyset id", e);
        }
    }

    /**
     * Builds the exact byte string NUT-02 hashes.
     *
     * <p>Kept separate from the hashing so a test can assert the bytes themselves. Hashing hides
     * every encoding mistake behind a digest that looks equally plausible either way, and a keyset
     * id that disagrees with other implementations by one byte is a keyset no other wallet can
     * use.
     */
    static byte[] preimage(Map<BigInteger, byte[]> keys,
                           String unit,
                           Integer inputFeePpk,
                           Long finalExpiry) {
        StringJoiner keyPairs = new StringJoiner(",");
        for (Map.Entry<BigInteger, byte[]> key : new TreeMap<>(keys).entrySet()) {
            keyPairs.add(key.getKey().toString() + ":" + Utils.bytesToHexString(key.getValue()).toLowerCase());
        }

        StringBuilder preimage = new StringBuilder(keyPairs.toString());
        preimage.append("|unit:").append(unit.toLowerCase());
        // A zero fee is omitted rather than written as "0": the spec is explicit, and writing it
        // would give a fee-free keyset a different id from the one every other mint derives.
        if (inputFeePpk != null && inputFeePpk != 0) {
            preimage.append("|input_fee_ppk:").append(inputFeePpk);
        }
        if (finalExpiry != null && finalExpiry != 0) {
            preimage.append("|final_expiry:").append(finalExpiry);
        }
        return preimage.toString().getBytes(StandardCharsets.UTF_8);
    }
}
