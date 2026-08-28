package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.json.deserializer.RandomStringSecretDeserializer;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * A NUT-00 secret message: an arbitrary UTF-8 string carried in {@code Proof.secret}.
 *
 * <p>NUT-00 <em>recommends</em> a 64-character hex string generated from 32 random bytes, but it
 * does not require one, so a secret minted elsewhere may be any UTF-8 text. The secret string is
 * therefore stored verbatim, as its UTF-8 bytes, and {@link #toString()} returns exactly the string
 * that was read off the wire.
 *
 * <p>That storage convention is deliberately the same one
 * {@code xyz.tcheeric.cashu.crypto.SecretEncoding#SPEC} uses when hashing: {@link #getData()}
 * returns the bytes that {@code hash_to_curve} consumes, for every secret. For the recommended hex
 * secret those are its 64 ASCII characters, not the 32 bytes they decode to. Callers that need the
 * decoded entropy of a hex secret must decode {@link #toString()} themselves.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/00.md">NUT-00</a>
 */
@JsonDeserialize(using = RandomStringSecretDeserializer.class)
public class RandomStringSecret extends BaseKey implements Secret {

    /**
     * Number of random bytes behind the 64-character hex secret NUT-00 recommends.
     */
    private static final int RECOMMENDED_ENTROPY_BYTES = 32;

    private RandomStringSecret(@NonNull String value) {
        super(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a secret from {@value #RECOMMENDED_ENTROPY_BYTES} random bytes, hex-encoded as
     * NUT-00 recommends.
     */
    public static RandomStringSecret create() {
        return create(RECOMMENDED_ENTROPY_BYTES);
    }

    /**
     * Generates a secret from {@code entropyLength} random bytes, hex-encoded as NUT-00 recommends.
     *
     * @param entropyLength number of random bytes to draw, before hex encoding
     */
    public static RandomStringSecret create(int entropyLength) {
        if (entropyLength <= 0) {
            throw new IllegalArgumentException("Entropy length must be positive, got: " + entropyLength);
        }
        byte[] entropy = new byte[entropyLength];
        new SecureRandom().nextBytes(entropy);
        return fromEntropy(entropy);
    }

    /**
     * Returns the UTF-8 bytes of the secret string, which are the bytes {@code hash_to_curve}
     * consumes.
     */
    @Override
    public byte[] getData() {
        return this.getBytes();
    }

    @Override
    public void setData(@NonNull byte[] data) {
        // Secrets are immutable once read: rewriting one would silently change the proof it locks.
    }

    /**
     * Reads a secret exactly as it appeared on the wire. Any UTF-8 string is accepted.
     */
    public static RandomStringSecret fromString(@NonNull String s) {
        return new RandomStringSecret(s);
    }

    /**
     * Builds a secret from raw entropy by hex-encoding it into the NUT-00 recommended form.
     *
     * @param entropy the random bytes behind the secret, <em>not</em> the UTF-8 bytes of a secret
     *                string; use {@link #fromString(String)} for the latter
     */
    public static RandomStringSecret fromEntropy(@NonNull byte[] entropy) {
        return fromString(Hex.toHexString(entropy));
    }

    /**
     * Builds a secret from raw entropy by hex-encoding it.
     *
     * @deprecated ambiguous name: use {@link #fromEntropy(byte[])} for random bytes, or
     * {@link #fromString(String)} for a secret string.
     */
    @Deprecated(forRemoval = true)
    public static RandomStringSecret fromBytes(byte[] bytes) {
        return fromEntropy(bytes);
    }

    public static RandomStringSecret fromBigInteger(@NonNull BigInteger b) {
        return fromEntropy(b.toByteArray());
    }

    /**
     * Returns the secret string itself, not a hex rendering of its bytes.
     */
    @JsonValue
    @Override
    public String toString() {
        byte[] bytes = getBytes();
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }
}
