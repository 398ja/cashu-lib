package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.util.KeysUtils;

/**
 * Represents a secp256k1 private key.
 *
 * <p>This class is annotated with {@code @JsonIgnoreType} to prevent accidental
 * serialization of private keys to JSON. Private keys should never appear in
 * API responses or logs.
 *
 * <p>To create a PrivateKey from a hex string (e.g., from configuration),
 * use the {@link #fromString(String)} factory method directly.
 *
 * <h2>Security Considerations</h2>
 *
 * <p><b>Memory Zeroing:</b> This class implements {@link AutoCloseable} to support
 * zeroing key material when the key is no longer needed. Use try-with-resources
 * for automatic cleanup:
 *
 * <pre>{@code
 * try (PrivateKey key = PrivateKey.generateRandom()) {
 *     // Use the key
 *     PublicKey pubKey = PrivateKey.derivePublicKey(key);
 * } // Key bytes are zeroed here
 * }</pre>
 *
 * <p><b>Limitations:</b> While {@link #close()} attempts to clear key material,
 * the JVM may retain copies in memory due to:
 * <ul>
 *   <li>Garbage collector behavior and memory compaction</li>
 *   <li>JIT optimizations that may eliminate zeroing operations</li>
 *   <li>String interning or logging that captured the key value</li>
 *   <li>Copies made during cryptographic operations</li>
 * </ul>
 *
 * <p>For highest security requirements, consider hardware security modules (HSMs).
 */
@JsonIgnoreType
public final class PrivateKey extends BaseKey implements AutoCloseable {

    /**
     * Tracks whether this key has been closed (zeroed).
     */
    private volatile boolean closed = false;

    protected PrivateKey(@NonNull String value) {
        this(Hex.decode(value));
    }

    protected PrivateKey(byte[] value) {
        super(value);
        if (value.length != PRIVATE_KEY_HEX_LENGTH / 2) {
            throw new IllegalArgumentException("Invalid private key length: " + value.length +
                    ". Expected " + (PRIVATE_KEY_HEX_LENGTH / 2) + " bytes.");
        }
    }

    public static PrivateKey fromString(@NonNull String s) {
        return new PrivateKey(s);
    }

    public static PrivateKey fromBytes(byte[] bytes) {
        return new PrivateKey(bytes);
    }

    public static PublicKey derivePublicKey(@NonNull PrivateKey privateKey) {
        return PublicKey.fromString(Hex.toHexString(KeysUtils.derivePublicKey(privateKey.getBytes())));
    }

    public static PrivateKey generateRandom() {
        return PrivateKey.fromBytes(KeysUtils.generatePrivateKey());
    }

    /**
     * Returns the key bytes.
     *
     * @return copy of the key bytes
     * @throws IllegalStateException if the key has been closed
     */
    @Override
    public byte[] getBytes() {
        if (closed) {
            throw new IllegalStateException("PrivateKey has been closed and zeroed");
        }
        return super.getBytes();
    }

    /**
     * Zeros the private key bytes to clear sensitive material from memory.
     *
     * <p>After calling this method, the key is no longer usable and {@link #getBytes()}
     * will throw {@link IllegalStateException}.
     *
     * <p>This method is idempotent - calling it multiple times has no additional effect.
     *
     * @see BaseKey#zeroBytes() for limitations of memory zeroing in Java
     */
    @Override
    public void close() {
        if (!closed) {
            zeroBytes();
            closed = true;
        }
    }

    /**
     * Returns whether this key has been closed and zeroed.
     *
     * @return true if the key has been closed
     */
    public boolean isClosed() {
        return closed;
    }

}