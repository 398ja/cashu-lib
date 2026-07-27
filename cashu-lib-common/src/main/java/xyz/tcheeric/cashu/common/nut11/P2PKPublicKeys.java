package xyz.tcheeric.cashu.common.nut11;

import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.crypto.util.Point;

import java.util.Arrays;
import java.util.Locale;

/**
 * NUT-11 public-key validation and comparison.
 *
 * <p>Deliberately stricter than {@link PublicKey}: {@code PublicKey.fromString} accepts 66 <em>or</em>
 * 128 hex chars and {@code fromBytes} accepts 33, 64 or 65 bytes, whereas NUT-11 mandates the
 * compressed 33-byte form only. Validating through {@code PublicKey} alone would let an
 * uncompressed key through.
 *
 * <p>It is also stricter about <em>when</em>: {@code PublicKey}'s constructor checks length and
 * parity prefix but stores the bytes without decompressing, so a syntactically valid key that is
 * not a point on secp256k1 is accepted and only fails later, on an unrelated path. Here the curve
 * check is eager.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/11.md">NUT-11</a>
 */
public final class P2PKPublicKeys {

    /** Compressed public key: 33 bytes. */
    private static final int COMPRESSED_BYTES = 33;

    /** Compressed public key as hex: 66 chars. */
    private static final int COMPRESSED_HEX_LENGTH = 66;

    private P2PKPublicKeys() {
    }

    /**
     * Validates a hex-encoded NUT-11 public key.
     *
     * <p>Case is normalised rather than rejected: NUT-11 lists uppercase and mixed-case hex as
     * valid, equivalent encodings of the same key.
     *
     * @param hex      the key, 66 hex chars with an {@code 02} or {@code 03} prefix
     * @param position where the key appeared, for the error message (e.g. {@code pubkeys[1]})
     * @throws MalformedP2PKSecretException if the key is not a valid compressed secp256k1 point
     */
    public static PublicKey requireValid(String hex, String position) {
        if (hex == null) {
            throw new MalformedP2PKSecretException(position + ": public key is missing");
        }
        String normalized = hex.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() != COMPRESSED_HEX_LENGTH) {
            throw new MalformedP2PKSecretException(position
                    + ": expected a compressed public key of " + COMPRESSED_HEX_LENGTH
                    + " hex chars, got " + normalized.length());
        }
        byte[] bytes;
        try {
            bytes = Hex.decode(normalized);
        } catch (RuntimeException e) {
            throw new MalformedP2PKSecretException(position + ": public key is not valid hex", e);
        }
        return requireValid(bytes, position);
    }

    /**
     * Validates a NUT-11 public key in compressed byte form.
     *
     * @param key      33 bytes, {@code 0x02} or {@code 0x03} prefix followed by the x-coordinate
     * @param position where the key appeared, for the error message
     * @throws MalformedP2PKSecretException if the key is not a valid compressed secp256k1 point
     */
    public static PublicKey requireValid(byte[] key, String position) {
        if (key == null) {
            throw new MalformedP2PKSecretException(position + ": public key is missing");
        }
        if (key.length != COMPRESSED_BYTES) {
            throw new MalformedP2PKSecretException(position
                    + ": expected a compressed public key of " + COMPRESSED_BYTES
                    + " bytes, got " + key.length);
        }
        byte prefix = key[0];
        if (prefix != 0x02 && prefix != 0x03) {
            throw new MalformedP2PKSecretException(position
                    + ": expected a compressed public key prefix of 0x02 or 0x03, got "
                    + String.format("0x%02x", prefix));
        }
        requireOnCurve(key, position);
        try {
            return PublicKey.fromBytes(key);
        } catch (RuntimeException e) {
            throw new MalformedP2PKSecretException(position + ": public key is not usable", e);
        }
    }

    /**
     * The form in which NUT-11 compares two keys: the lowercase x-coordinate, parity prefix
     * dropped.
     *
     * <p>This is required, not defensive. BIP-340 signs on the x-coordinate alone, so {@code 02||x}
     * and {@code 03||x} are one signing key — and NUT-11 declares them duplicates of each other,
     * fatal within a single pathway. Comparing full compressed hex would miss that.
     *
     * <p>Lenient by design: callers pass keys that {@link #requireValid} has already accepted.
     */
    public static String toComparisonForm(String hex) {
        if (hex == null) {
            throw new MalformedP2PKSecretException("public key is missing");
        }
        String normalized = hex.trim().toLowerCase(Locale.ROOT);
        return normalized.length() == COMPRESSED_HEX_LENGTH ? normalized.substring(2) : normalized;
    }

    /**
     * Eager curve-membership check. {@link Point#liftX} returns null for an x-coordinate that is
     * not on secp256k1, and may throw for one outside the field; both mean the same thing here.
     */
    private static void requireOnCurve(byte[] key, String position) {
        Point point;
        try {
            point = Point.liftX(Arrays.copyOfRange(key, 1, key.length));
        } catch (RuntimeException e) {
            throw new MalformedP2PKSecretException(position
                    + ": public key is not a point on secp256k1", e);
        }
        if (point == null) {
            throw new MalformedP2PKSecretException(position
                    + ": public key is not a point on secp256k1");
        }
    }
}
