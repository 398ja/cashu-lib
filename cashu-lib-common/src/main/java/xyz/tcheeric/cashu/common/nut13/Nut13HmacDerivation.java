package xyz.tcheeric.cashu.common.nut13;

import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * NUT-13 deterministic derivation for version 2 keysets.
 *
 * <p>Version 1 keysets derive secrets through BIP32; version 2 keysets use this HMAC-SHA256 KDF
 * instead. Which one applies is decided by the keyset id's version byte, and using the wrong one
 * silently produces secrets that recover nothing, since a wallet cannot tell an empty recovery
 * from a wrongly-derived one.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/13.md">NUT-13</a>
 */
public final class Nut13HmacDerivation {

    /** Domain separation string, so this KDF's output cannot collide with another use of the seed. */
    private static final byte[] PURPOSE = "Cashu_KDF_HMAC_SHA256".getBytes(StandardCharsets.UTF_8);

    private static final String HMAC_SHA256 = "HmacSHA256";

    /** Distinguishes a secret from a blinding factor derived at the same counter. */
    private static final byte SECRET = 0x00;
    private static final byte BLINDING_FACTOR = 0x01;

    /** Width of a secp256k1 scalar, which a blinding factor is always rendered at. */
    private static final int SCALAR_BYTES = 32;

    /** Order of the secp256k1 group, which a blinding factor is reduced into. */
    private static final BigInteger CURVE_ORDER = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

    private Nut13HmacDerivation() {
    }

    /** Derives the secret for a counter. */
    public static byte[] deriveSecret(@NonNull byte[] seed, @NonNull KeysetIdBytes keysetId, long counter) {
        return hmac(seed, message(keysetId, counter, SECRET));
    }

    /**
     * Derives the blinding factor for a counter.
     *
     * <p>Reduced modulo the curve order, because a raw digest is not necessarily a valid scalar.
     */
    public static byte[] deriveBlindingFactor(@NonNull byte[] seed,
                                              @NonNull KeysetIdBytes keysetId,
                                              long counter) {
        BigInteger digest = new BigInteger(1, hmac(seed, message(keysetId, counter, BLINDING_FACTOR)));
        return toFixedWidth(digest.mod(CURVE_ORDER));
    }

    /**
     * Renders a scalar as exactly 32 bytes.
     *
     * <p>{@link BigInteger#toByteArray()} prepends a sign byte when the leading bit is set and
     * drops leading zeroes otherwise, so the same scalar can come out 31, 32 or 33 bytes long. A
     * blinding factor is a fixed-width value, and a wallet that stores it at the wrong width
     * unblinds to nothing.
     */
    private static byte[] toFixedWidth(BigInteger scalar) {
        byte[] unpadded = scalar.toByteArray();
        if (unpadded.length == SCALAR_BYTES) {
            return unpadded;
        }
        byte[] fixed = new byte[SCALAR_BYTES];
        if (unpadded.length > SCALAR_BYTES) {
            System.arraycopy(unpadded, unpadded.length - SCALAR_BYTES, fixed, 0, SCALAR_BYTES);
        } else {
            System.arraycopy(unpadded, 0, fixed, SCALAR_BYTES - unpadded.length, unpadded.length);
        }
        return fixed;
    }

    private static byte[] message(KeysetIdBytes keysetId, long counter, byte derivationType) {
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        message.writeBytes(PURPOSE);
        message.writeBytes(keysetId.bytes());
        message.writeBytes(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
        message.write(derivationType);
        return message.toByteArray();
    }

    private static byte[] hmac(byte[] seed, byte[] message) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(seed, HMAC_SHA256));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is required for NUT-13 v2 derivation", e);
        }
    }

    /**
     * The raw bytes of a keyset id, hex-decoded.
     *
     * <p>A named type rather than a bare {@code byte[]} because the KDF commits to these bytes:
     * passing the hex string's characters instead would derive a different, equally plausible
     * secret, and the mistake would only surface as a recovery that finds nothing.
     */
    public record KeysetIdBytes(byte[] bytes) {

        public static KeysetIdBytes of(@NonNull String keysetIdHex) {
            return new KeysetIdBytes(Hex.decode(keysetIdHex));
        }
    }
}
