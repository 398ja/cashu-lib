package xyz.tcheeric.cashu.common.nut13;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.BaseKey;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.SecretDerivationPath;

/**
 * DeterministicSecret represents a secret derived from a BIP39 mnemonic using BIP32 derivation paths.
 * This implementation supports NUT-13 deterministic secret generation for wallet recovery.
 *
 * <p>Unlike {@link xyz.tcheeric.cashu.common.RandomStringSecret}, which generates secrets randomly, DeterministicSecret
 * stores secrets that are deterministically derived from a master key using the derivation path:
 * <pre>m/129372'/0'/{keyset_id_int}'/{counter}'/0</pre>
 *
 * <p>The secret bytes are derived using BIP32 hierarchical deterministic key derivation,
 * ensuring that the same mnemonic + derivation path always produces the same secret.
 *
 * <p>This class is immutable once created and optionally stores metadata about the derivation
 * path for debugging and auditing purposes.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/13.md">NUT-13 Specification</a>
 * @see SecretDerivationPath
 * @see xyz.tcheeric.cashu.common.RandomStringSecret
 *
 * @author NUT-13 Implementation Team
 * @since 1.0.0
 */
@JsonDeserialize(using = DeterministicSecretDeserializer.class)
public class DeterministicSecret extends BaseKey implements Secret {

    /**
     * Optional derivation path used to generate this secret.
     * Stored for debugging and auditing purposes.
     */
    @Getter
    private final SecretDerivationPath derivationPath;

    /**
     * The keyset ID this secret was derived for.
     */
    @Getter
    private final KeysetId keysetId;

    /**
     * The counter value used in the derivation path.
     */
    @Getter
    private final int counter;

    /**
     * Creates a DeterministicSecret from derived bytes without metadata.
     *
     * @param derivedBytes the deterministically derived secret bytes (typically 32 bytes)
     * @throws IllegalArgumentException if derivedBytes is null or empty
     */
    private DeterministicSecret(@NonNull byte[] derivedBytes) {
        this(derivedBytes, null, null, 0);
    }

    /**
     * Creates a DeterministicSecret from derived bytes with full metadata.
     *
     * @param derivedBytes the deterministically derived secret bytes (typically 32 bytes)
     * @param keysetId the keyset ID used in derivation
     * @param counter the counter value used in derivation
     * @param derivationPath the full derivation path (optional)
     * @throws IllegalArgumentException if derivedBytes is null or empty
     */
    private DeterministicSecret(
            @NonNull byte[] derivedBytes,
            KeysetId keysetId,
            SecretDerivationPath derivationPath,
            int counter) {
        super(derivedBytes);
        this.keysetId = keysetId;
        this.counter = counter;
        this.derivationPath = derivationPath;

        // Validate derivation path consistency if provided
        if (derivationPath != null && keysetId != null) {
            if (!derivationPath.getKeysetId().equals(keysetId)) {
                throw new IllegalArgumentException(
                    "Derivation path keyset ID does not match provided keyset ID");
            }
            if (derivationPath.getCounter() != counter) {
                throw new IllegalArgumentException(
                    "Derivation path counter does not match provided counter");
            }
        }
    }

    /**
     * Creates a DeterministicSecret from hex-encoded string.
     * This is primarily used for deserialization.
     *
     * @param hexString the hex-encoded secret (without 0x prefix)
     * @return a new DeterministicSecret instance
     * @throws IllegalArgumentException if hexString is invalid
     */
    public static DeterministicSecret fromString(@NonNull String hexString) {
        byte[] bytes = org.bouncycastle.util.encoders.Hex.decode(hexString);
        return new DeterministicSecret(bytes);
    }

    /**
     * Creates a DeterministicSecret from raw bytes.
     *
     * @param bytes the secret bytes
     * @return a new DeterministicSecret instance
     * @throws IllegalArgumentException if bytes is null or empty
     */
    public static DeterministicSecret fromBytes(@NonNull byte[] bytes) {
        return new DeterministicSecret(bytes);
    }

    /**
     * Creates a DeterministicSecret with full metadata.
     * This is the recommended factory method for creating secrets during wallet operations.
     *
     * @param derivedBytes the deterministically derived secret bytes
     * @param keysetId the keyset ID used in derivation
     * @param counter the counter value used in derivation
     * @return a new DeterministicSecret instance with metadata
     * @throws IllegalArgumentException if derivedBytes or keysetId is null
     */
    public static DeterministicSecret create(
            @NonNull byte[] derivedBytes,
            @NonNull KeysetId keysetId,
            int counter) {
        // Create derivation path for metadata
        SecretDerivationPath path = new SecretDerivationPath();
        path.setKeysetId(keysetId);
        path.setCounter(counter);

        return new DeterministicSecret(derivedBytes, keysetId, path, counter);
    }

    /**
     * Creates a DeterministicSecret with derivation path.
     *
     * @param derivedBytes the deterministically derived secret bytes
     * @param derivationPath the derivation path used
     * @return a new DeterministicSecret instance
     * @throws IllegalArgumentException if derivedBytes or derivationPath is null
     */
    public static DeterministicSecret create(
            @NonNull byte[] derivedBytes,
            @NonNull SecretDerivationPath derivationPath) {
        return new DeterministicSecret(
            derivedBytes,
            derivationPath.getKeysetId(),
            derivationPath,
            derivationPath.getCounter()
        );
    }

    @Override
    public byte[] getData() {
        return this.getBytes();
    }

    @Override
    public void setData(@NonNull byte[] data) {
        // Deterministic secrets are immutable - data cannot be changed after creation
        throw new UnsupportedOperationException(
            "DeterministicSecret is immutable. Cannot modify secret data after creation.");
    }

    /**
     * Checks if this secret has derivation metadata.
     *
     * @return true if derivation path and keyset ID are present
     */
    public boolean hasMetadata() {
        return keysetId != null && derivationPath != null;
    }

    /**
     * Returns the hex-encoded secret without metadata.
     * This is used for JSON serialization (via @JsonValue) and network transmission.
     *
     * @return hex-encoded secret string
     */
    @com.fasterxml.jackson.annotation.JsonValue
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Returns a string representation including metadata if available.
     * Useful for debugging and logging.
     *
     * @return hex string representation with optional metadata
     */
    public String toStringWithMetadata() {
        String hexString = toString();
        if (hasMetadata()) {
            return String.format("%s (keyset=%s, counter=%d, path=%s)",
                hexString, keysetId, counter, derivationPath);
        }
        return hexString;
    }

    /**
     * Returns the hex-encoded secret without metadata.
     * Alias for toString() for clarity.
     *
     * @return hex-encoded secret string
     */
    public String toHexString() {
        return toString();
    }
}
