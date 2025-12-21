package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.bouncycastle.util.encoders.Hex;

/**
 * Base class for cryptographic keys in the Cashu protocol.
 *
 * <p>Provides common functionality for private keys, public keys, and secrets.
 * Keys are stored as raw byte arrays and serialized as lowercase hex strings.
 *
 * <p>Key length constants (in hex characters):
 * <ul>
 *   <li>{@link #PRIVATE_KEY_HEX_LENGTH}: 64 chars (32 bytes)</li>
 *   <li>{@link #X_COORDINATE_HEX_LENGTH}: 64 chars (32 bytes) - just the x-coordinate</li>
 *   <li>{@link #COMPRESSED_KEY_HEX_LENGTH}: 66 chars (33 bytes) - prefix + x-coordinate</li>
 *   <li>{@link #UNCOMPRESSED_XY_HEX_LENGTH}: 128 chars (64 bytes) - x || y coordinates</li>
 *   <li>{@link #SECRET_HEX_LENGTH}: 64 chars (32 bytes)</li>
 * </ul>
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
     * @deprecated Use {@link #PRIVATE_KEY_HEX_LENGTH} instead.
     */
    @Deprecated(forRemoval = true)
    protected static final int PRIVATE_KEY_LENGTH = PRIVATE_KEY_HEX_LENGTH;

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
     * @deprecated Misleading name. Use {@link #X_COORDINATE_HEX_LENGTH} for x-only
     *             or {@link #COMPRESSED_KEY_HEX_LENGTH} for full compressed key.
     */
    @Deprecated(forRemoval = true)
    protected static final int PUBLIC_KEY_LENGTH_COMPRESSED = X_COORDINATE_HEX_LENGTH;

    /**
     * Uncompressed public key coordinates length in hex characters (64 bytes = 128 hex chars).
     * Format: x-coordinate (64 chars) + y-coordinate (64 chars), without the 04 prefix.
     */
    protected static final int UNCOMPRESSED_XY_HEX_LENGTH = 128;

    /**
     * @deprecated Incorrect value (was 66, should be 128). Use {@link #UNCOMPRESSED_XY_HEX_LENGTH}.
     */
    @Deprecated(forRemoval = true)
    protected static final int PUBLIC_KEY_LENGTH_UNCOMPRESSED = 66;

    /**
     * Secret length in hex characters (32 bytes = 64 hex chars).
     */
    protected static final int SECRET_HEX_LENGTH = 64;

    /**
     * @deprecated Use {@link #SECRET_HEX_LENGTH} instead.
     */
    @Deprecated(forRemoval = true)
    protected static final int SECRET_LENGTH = SECRET_HEX_LENGTH;

    /**
     * The raw key bytes.
     */
    @EqualsAndHashCode.Include
    private byte[] bytes;

    /**
     * Creates a BaseKey from a hex string, stripping the first byte (prefix).
     *
     * @param hexStr hex string with prefix to strip
     * @deprecated This constructor strips the prefix, which is error-prone.
     *             Use byte[] constructor or subclass-specific methods instead.
     */
    @Deprecated(forRemoval = true)
    protected BaseKey(@NonNull String hexStr) {
        this.bytes = Hex.decode(hexStr.substring(2));
    }

    /**
     * Creates a BaseKey from raw bytes.
     *
     * @param bytes the key bytes
     */
    protected BaseKey(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Compares this key to another, ignoring case differences in hex representation.
     *
     * @param key the key to compare
     * @return true if the keys are equal (case-insensitive)
     */
    public boolean equalsIgnoreCase(@NonNull BaseKey key) {
        return this.toString().equalsIgnoreCase(key.toString());
    }

    /**
     * Returns the key as a lowercase hex string.
     *
     * @return hex string representation
     */
    @JsonValue
    @Override
    public String toString() {
        return Hex.toHexString(bytes);
    }

    /**
     * Returns a copy of the key bytes.
     *
     * @return copy of the key bytes
     * @deprecated Use {@link #getBytes()} instead.
     */
    @Deprecated(forRemoval = true)
    public byte[] toBytes() {
        return bytes;
    }
}
