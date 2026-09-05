package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * Base class for cryptographic keys in the Cashu protocol.
 *
 * <p>Provides common functionality for private keys, public keys, and secrets.
 * Keys are stored as raw byte arrays and serialized as lowercase hex strings.
 *
 * <h2>Key Length Constants</h2>
 * <p>All lengths are in hex characters:
 * <ul>
 *   <li>{@link #PRIVATE_KEY_HEX_LENGTH}: 64 chars (32 bytes)</li>
 *   <li>{@link #X_COORDINATE_HEX_LENGTH}: 64 chars (32 bytes) - just the x-coordinate</li>
 *   <li>{@link #COMPRESSED_KEY_HEX_LENGTH}: 66 chars (33 bytes) - prefix + x-coordinate</li>
 *   <li>{@link #UNCOMPRESSED_XY_HEX_LENGTH}: 128 chars (64 bytes) - x || y coordinates</li>
 *   <li>{@link #SECRET_HEX_LENGTH}: 64 chars (32 bytes)</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><b>Immutability:</b> This class uses defensive copying in constructors
 *       and getters to prevent external modification of key bytes. However,
 *       the internal state can be modified by subclasses via {@link #setBytes}.</li>
 *   <li><b>Memory Zeroing:</b> Subclasses handling sensitive data (like
 *       {@link PrivateKey}) should implement {@link AutoCloseable} and call
 *       {@link #zeroBytes()} to clear key material when no longer needed.</li>
 *   <li><b>Serialization:</b> The {@code @JsonValue} annotation on {@link #toString()}
 *       means key bytes will be serialized to JSON. Sensitive subclasses should
 *       use {@code @JsonIgnoreType} to prevent accidental serialization.</li>
 *   <li><b>Equality:</b> Key equality is based on byte content, not identity.
 *       Two key objects with the same bytes are considered equal.</li>
 * </ul>
 *
 * @see PrivateKey
 * @see PublicKey
 * @see Signature
 */
@Getter
@Setter(AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public abstract class BaseKey {

    /**
     * Private key length in hex characters (32 bytes = 64 hex chars).
     */
    protected static final int PRIVATE_KEY_HEX_LENGTH = 64;

    /**
     * X-coordinate length in hex characters (32 bytes = 64 hex chars).
     * This is the raw x-coordinate without the compression prefix.
     */
    protected static final int X_COORDINATE_HEX_LENGTH = 64;

    /**
     * Compressed public key length in hex characters (33 bytes = 66 hex chars).
     * Format: 02/03 prefix (2 chars) + x-coordinate (64 chars).
     */
    protected static final int COMPRESSED_KEY_HEX_LENGTH = 66;

    /**
    /**
     * Uncompressed public key coordinates length in hex characters (64 bytes = 128 hex chars).
     * Format: x-coordinate (64 chars) + y-coordinate (64 chars), without the 04 prefix.
     */
    protected static final int UNCOMPRESSED_XY_HEX_LENGTH = 128;

    /**
     * Secret length in hex characters (32 bytes = 64 hex chars).
     */
    protected static final int SECRET_HEX_LENGTH = 64;

    /**
     * The raw key bytes.
     */
    @Getter(AccessLevel.NONE)  // Explicit getBytes() provides defensive copy
    @Setter(AccessLevel.NONE)  // Explicit setBytes() provides defensive copy
    @EqualsAndHashCode.Include
    private byte[] bytes;

    /**
     * Creates a BaseKey from raw bytes.
     *
     * <p>The input array is defensively copied to prevent external modification.
     *
     * @param bytes the key bytes
     */
    protected BaseKey(byte[] bytes) {
        this.bytes = bytes != null ? Arrays.copyOf(bytes, bytes.length) : null;
    }

    /**
     * Returns a copy of the key bytes.
     *
     * <p>A defensive copy is returned to prevent external modification of internal state.
     *
     * @return copy of the key bytes
     */
    public byte[] getBytes() {
        return bytes != null ? Arrays.copyOf(bytes, bytes.length) : null;
    }

    /**
     * Sets the key bytes with defensive copying.
     *
     * <p>The input array is defensively copied to prevent external modification.
     *
     * @param bytes the key bytes to set
     */
    protected void setBytes(byte[] bytes) {
        this.bytes = bytes != null ? Arrays.copyOf(bytes, bytes.length) : null;
    }

    /**
     * Zeros out the internal key bytes for security purposes.
     *
     * <p>This method attempts to clear sensitive key material from memory.
     * While this provides defense-in-depth, be aware of the following limitations:
     * <ul>
     *   <li>The JVM may have created copies of the key bytes during operations</li>
     *   <li>The garbage collector may retain copies in freed memory</li>
     *   <li>JIT compilation may optimize away the zeroing operation</li>
     *   <li>The key bytes may have been logged or serialized elsewhere</li>
     * </ul>
     *
     * <p>For highest security, consider using hardware security modules (HSMs)
     * or secure enclaves for key storage.
     */
    protected void zeroBytes() {
        if (bytes != null) {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    /**
     * Compares this key to another, ignoring case differences in hex representation.
     *
     * @param key the key to compare
     * @return true if the keys are equal (case-insensitive)
     */
    public boolean equalsIgnoreCase(@NonNull BaseKey key) {
        return this.asHex().equalsIgnoreCase(key.asHex());
    }

    /**
     * Returns the key as a lowercase hex string.
     *
     * <p>This is the wire form. It is deliberately separate from {@link #toString()} so that a
     * subclass holding secret material can redact its {@code toString()} without losing the
     * ability to serialize. Call this only where the hex is genuinely needed (JSON, storage,
     * signing), never in a log statement or an exception message.
     *
     * @return hex string representation
     */
    public String asHex() {
        return Hex.toHexString(bytes);
    }

    /**
     * Returns the key as a lowercase hex string.
     *
     * <p>Overridden by secret-bearing subclasses to redact. See {@link #asHex()} for the
     * unconditional wire form.
     *
     * @return hex string representation
     */
    @JsonValue
    @Override
    public String toString() {
        return asHex();
    }

    /**
     * Returns a copy of the key bytes.
     *
     * @return copy of the key bytes
     * @deprecated Use {@link #getBytes()} instead.
     */
    @Deprecated(forRemoval = true)
    public byte[] toBytes() {
        return getBytes();
    }
}
