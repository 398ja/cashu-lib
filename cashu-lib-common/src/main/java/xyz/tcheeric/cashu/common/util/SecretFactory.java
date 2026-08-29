package xyz.tcheeric.cashu.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bitcoinj.crypto.DeterministicKey;
import xyz.tcheeric.bips.bip32.nut.Nut13Derivation;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.KeysetIdVersion;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;
import xyz.tcheeric.cashu.common.nut13.DeterministicSecret;
import xyz.tcheeric.cashu.common.nut13.Nut13HmacDerivation;
import xyz.tcheeric.cashu.common.nut13.UnsupportedKeysetVersionException;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating different types of secrets (random, P2PK, and deterministic).
 *
 * <p>Supports three types of secrets:
 * <ul>
 *   <li>{@link RandomStringSecret} - Randomly generated secrets (default)</li>
 *   <li>{@link P2PKSecret} - Pay-to-public-key secrets</li>
 *   <li>{@link DeterministicSecret} - NUT-13 deterministically derived secrets</li>
 * </ul>
 *
 * @param <T> The type of secret to create
 * @author NUT-13 Implementation Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecretFactory<T extends Secret> {

    private static final KeysetIdVersion DERIVABLE_KEYSET_ID_VERSION = KeysetIdVersion.V1;

    private byte[] p2pkPublicKey;

    public T create() {
        if (p2pkPublicKey == null) {
            return (T) RandomStringSecret.create();
        }

        return (T) new P2PKSecret(p2pkPublicKey);
    }

    public static <T extends Secret> T create(byte[] p2pkPublicKey) {
        return (T) new SecretFactory<T>(p2pkPublicKey).create();
    }

    // ==================== NUT-13 Deterministic Secret Methods ====================

    /**
     * Creates a deterministic secret using NUT-13 derivation from a master key.
     *
     * <p>This method derives a secret using the BIP32 hierarchical deterministic key derivation
     * with the NUT-13 path: {@code m/129372'/0'/{keyset_id_int}'/{counter}'/0}
     *
     * @param masterKey BIP32 master key derived from BIP39 mnemonic
     * @param keysetId Keyset ID (hex string)
     * @param counter Counter value for derivation (typically starts at 0, increments per mint)
     * @return DeterministicSecret with derived bytes and metadata
     * @throws NullPointerException if masterKey or keysetId is null
     * @see xyz.tcheeric.bips.bip32.nut.Nut13Derivation#deriveSecret(DeterministicKey, String, int)
     */
    public static DeterministicSecret createDeterministic(
            @NonNull DeterministicKey masterKey,
            @NonNull KeysetId keysetId,
            int counter
    ) {
        requireDerivableKeysetVersion(keysetId);

        byte[] secretBytes = Nut13Derivation.deriveSecret(
                masterKey,
                keysetId.toString(),
                counter
        );

        return DeterministicSecret.create(secretBytes, keysetId, counter);
    }

    /**
     * Creates a batch of deterministic secrets for a keyset with sequential counters.
     *
     * <p>This is useful for generating multiple secrets at once, such as for wallet recovery
     * where secrets need to be derived in batches of 100 per the NUT-13 specification.
     *
     * @param masterKey BIP32 master key
     * @param keysetId Keyset ID
     * @param startCounter Starting counter value (inclusive)
     * @param count Number of secrets to generate
     * @return List of deterministic secrets with counters from startCounter to startCounter+count-1
     * @throws NullPointerException if masterKey or keysetId is null
     * @throws IllegalArgumentException if count is negative
     */
    public static List<DeterministicSecret> createDeterministicBatch(
            @NonNull DeterministicKey masterKey,
            @NonNull KeysetId keysetId,
            int startCounter,
            int count
    ) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be non-negative, got: " + count);
        }

        List<DeterministicSecret> secrets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            secrets.add(createDeterministic(masterKey, keysetId, startCounter + i));
        }
        return secrets;
    }

    /**
     * Creates a deterministic secret directly from a BIP39 mnemonic phrase (convenience method).
     *
     * <p>This is a convenience method that combines mnemonic-to-seed conversion, master key
     * derivation, and secret derivation into a single call. For generating multiple secrets,
     * it's more efficient to derive the master key once and use {@link #createDeterministic}
     * or {@link #createDeterministicBatch}.
     *
     * @param mnemonic BIP39 mnemonic phrase (12, 15, 18, 21, or 24 words)
     * @param passphrase Optional BIP39 passphrase (use empty string "" for none)
     * @param keysetId Keyset ID
     * @param counter Counter value for derivation
     * @return DeterministicSecret
     * @throws NullPointerException if mnemonic, passphrase, or keysetId is null
     * @throws IllegalArgumentException if mnemonic is invalid
     * @see xyz.tcheeric.bips.bip32.nut.Nut13Derivation#deriveSecretFromMnemonic
     */
    public static DeterministicSecret createDeterministicFromMnemonic(
            @NonNull String mnemonic,
            @NonNull String passphrase,
            @NonNull KeysetId keysetId,
            int counter
    ) {
        requireDerivableKeysetVersion(keysetId);

        var params = Nut13Derivation.Nut13DerivationParams.builder()
                .mnemonicPhrase(mnemonic)
                .passphrase(passphrase)
                .keysetIdHex(keysetId.toString())
                .counter(counter)
                .build();

        byte[] secretBytes = Nut13Derivation.deriveSecretFromMnemonic(params);
        return DeterministicSecret.create(secretBytes, keysetId, counter);
    }

    /**
     * Derives both secret and blinding factor for a deterministic token.
     *
     * <p>This method derives both the secret (index 0) and blinding factor (index 1) in one call,
     * which is useful for minting operations where both are needed.
     *
     * @param masterKey BIP32 master key
     * @param keysetId Keyset ID
     * @param counter Counter value
     * @return SecretAndBlindingFactor containing both derived values
     * @throws NullPointerException if masterKey or keysetId is null
     */
    public static SecretAndBlindingFactor createDeterministicWithBlindingFactor(
            @NonNull DeterministicKey masterKey,
            @NonNull KeysetId keysetId,
            int counter
    ) {
        requireDerivableKeysetVersion(keysetId);

        var pair = Nut13Derivation.deriveSecretAndBlindingFactor(
                masterKey,
                keysetId.toString(),
                counter
        );

        DeterministicSecret secret = DeterministicSecret.create(
                pair.secret(),
                keysetId,
                counter
        );

        return new SecretAndBlindingFactor(secret, pair.blindingFactor());
    }

    /**
     * Derives a secret for a keyset of either version, choosing the derivation its version
     * requires.
     *
     * <p>Version 1 keysets derive through BIP32 from the master key; version 2 keysets use the
     * HMAC-SHA256 KDF over the seed. Both are needed because the wrong one recovers nothing while
     * looking exactly like an empty wallet.
     *
     * @param seed      the BIP39 seed, required for a version 2 keyset
     * @param masterKey the BIP32 master key derived from that seed, required for version 1
     */
    public static DeterministicSecret createDeterministic(@NonNull byte[] seed,
                                                          @NonNull DeterministicKey masterKey,
                                                          @NonNull KeysetId keysetId,
                                                          int counter) {
        if (keysetId.getVersion() == DERIVABLE_KEYSET_ID_VERSION) {
            return createDeterministic(masterKey, keysetId, counter);
        }
        byte[] secretBytes = Nut13HmacDerivation.deriveSecret(
                seed, Nut13HmacDerivation.KeysetIdBytes.of(keysetId.toString()), counter);
        return DeterministicSecret.create(secretBytes, keysetId, counter);
    }

    /**
     * Rejects keyset ids the master-key derivation cannot serve.
     *
     * <p>Only version 1 ids derive through BIP32. A version 2 keyset needs the seed rather than
     * the master key, so it is refused here and served by the overload that takes one: deriving
     * version 1 secrets for a version 2 keyset would recover nothing while looking like an empty
     * wallet.
     *
     * @param keysetId keyset id to check
     * @throws UnsupportedKeysetVersionException if the id is not a version 1 keyset id
     */
    private static void requireDerivableKeysetVersion(@NonNull KeysetId keysetId) {
        KeysetIdVersion version = keysetId.getVersion();
        if (version != DERIVABLE_KEYSET_ID_VERSION) {
            throw new UnsupportedKeysetVersionException(keysetId, version);
        }
    }

    /**
     * Record containing both a deterministic secret and its corresponding blinding factor.
     *
     * @param secret The derived deterministic secret
     * @param blindingFactor The derived blinding factor (r value)
     */
    public record SecretAndBlindingFactor(
            DeterministicSecret secret,
            byte[] blindingFactor
    ) {
        /**
         * Returns the counter value from the secret's metadata.
         *
         * @return counter value
         */
        public int getCounter() {
            return secret.getCounter();
        }

        /**
         * Returns the keyset ID from the secret's metadata.
         *
         * @return keyset ID
         */
        public KeysetId getKeysetId() {
            return secret.getKeysetId();
        }
    }
}
