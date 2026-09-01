package xyz.tcheeric.cashu.common.util;

import org.bitcoinj.crypto.DeterministicKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import xyz.tcheeric.bips.bip39.Bip39;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.nut13.DeterministicSecret;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for SecretFactory NUT-13 deterministic secret generation methods.
 */
class SecretFactoryNUT13Test {

    private static final String TEST_MNEMONIC =
            "abandon abandon abandon abandon abandon abandon " +
                    "abandon abandon abandon abandon abandon about";
    private static final String TEST_PASSPHRASE = "";
    private static final String TEST_KEYSET_ID = "009a1f293253e41e";

    /**
     * Ensures deterministic secrets contain metadata and a derivation path.
     */
    @Test
    void shouldCreateDeterministicSecretWithMetadata() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        int counter = 0;

        // Act
        DeterministicSecret secret = SecretFactory.createDeterministic(masterKey, keysetId, counter);

        // Assert
        assertNotNull(secret);
        assertEquals(32, secret.getDerivedBytes().length);
        assertTrue(secret.hasMetadata());
        assertEquals(keysetId, secret.getKeysetId());
        assertEquals(counter, secret.getCounter());
        assertNotNull(secret.getDerivationPath());
    }

    /**
     * Ensures deterministic secret derivation is reproducible for identical inputs.
     */
    @Test
    void shouldCreateIdenticalSecretsForSameInputs() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        // Act
        DeterministicSecret secretOne = SecretFactory.createDeterministic(masterKey, keysetId, 0);
        DeterministicSecret secretTwo = SecretFactory.createDeterministic(masterKey, keysetId, 0);

        // Assert
        assertEquals(secretOne, secretTwo);
        assertArrayEquals(secretOne.getDerivedBytes(), secretTwo.getDerivedBytes());
        assertEquals(secretOne.toHexString(), secretTwo.toHexString());
    }

    /**
     * Ensures different counters yield unique deterministic secrets.
     */
    @Test
    void shouldProduceDifferentSecretsWhenCounterDiffers() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        // Act
        DeterministicSecret counterZeroSecret = SecretFactory.createDeterministic(masterKey, keysetId, 0);
        DeterministicSecret counterOneSecret = SecretFactory.createDeterministic(masterKey, keysetId, 1);

        // Assert
        assertNotEquals(counterZeroSecret, counterOneSecret);
        assertFalse(Arrays.equals(counterZeroSecret.getDerivedBytes(), counterOneSecret.getDerivedBytes()));
    }

    /**
     * Ensures different keysets produce distinct secrets.
     */
    @Test
    void shouldProduceDifferentSecretsWhenKeysetDiffers() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId firstKeyset = KeysetId.fromString(TEST_KEYSET_ID);
        KeysetId secondKeyset = KeysetId.fromString("00bf9e9d1f935a41");

        // Act
        DeterministicSecret firstSecret = SecretFactory.createDeterministic(masterKey, firstKeyset, 0);
        DeterministicSecret secondSecret = SecretFactory.createDeterministic(masterKey, secondKeyset, 0);

        // Assert
        assertNotEquals(firstSecret, secondSecret);
        assertFalse(Arrays.equals(firstSecret.getDerivedBytes(), secondSecret.getDerivedBytes()));
    }

    /**
     * Ensures a null master key triggers a helpful NullPointerException.
     */
    @Test
    void shouldRejectNullMasterKey() {
        // Arrange
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        Executable action = () -> SecretFactory.createDeterministic(null, keysetId, 0);

        // Act & Assert
        assertThrows(NullPointerException.class, action);
    }

    /**
     * Ensures a null keyset id triggers a helpful NullPointerException.
     */
    @Test
    void shouldRejectNullKeysetId() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        Executable action = () -> SecretFactory.createDeterministic(masterKey, null, 0);

        // Act & Assert
        assertThrows(NullPointerException.class, action);
    }

    /**
     * Ensures batches contain sequential counters with preserved metadata.
     */
    @Test
    void shouldCreateSequentialBatch() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        int startCounter = 0;
        int count = 10;

        // Act
        List<DeterministicSecret> secrets =
                SecretFactory.createDeterministicBatch(masterKey, keysetId, startCounter, count);

        // Assert
        assertNotNull(secrets);
        assertEquals(count, secrets.size());
        for (int index = 0; index < count; index++) {
            DeterministicSecret secret = secrets.get(index);
            assertEquals(startCounter + index, secret.getCounter());
            assertEquals(keysetId, secret.getKeysetId());
            assertTrue(secret.hasMetadata());
        }
        for (int firstIndex = 0; firstIndex < count; firstIndex++) {
            for (int secondIndex = firstIndex + 1; secondIndex < count; secondIndex++) {
                assertNotEquals(secrets.get(firstIndex), secrets.get(secondIndex));
            }
        }
    }

    /**
     * Ensures the recommended batch size of 100 secrets covers the expected counter range.
     */
    @Test
    void shouldCreateHundredSecretsForRecommendedBatch() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        // Act
        List<DeterministicSecret> secrets =
                SecretFactory.createDeterministicBatch(masterKey, keysetId, 0, 100);

        // Assert
        assertEquals(100, secrets.size());
        assertEquals(0, secrets.get(0).getCounter());
        assertEquals(99, secrets.get(99).getCounter());
    }

    /**
     * Ensures a zero-count batch returns an empty list.
     */
    @Test
    void shouldReturnEmptyBatchWhenCountZero() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        // Act
        List<DeterministicSecret> secrets =
                SecretFactory.createDeterministicBatch(masterKey, keysetId, 0, 0);

        // Assert
        assertNotNull(secrets);
        assertTrue(secrets.isEmpty());
    }

    /**
     * Ensures negative batch sizes are rejected.
     */
    @Test
    void shouldRejectNegativeBatchCount() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        Executable action = () -> SecretFactory.createDeterministicBatch(masterKey, keysetId, 0, -1);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, action);
    }

    /**
     * Ensures deterministic secrets can be built directly from mnemonics.
     */
    @Test
    void shouldCreateDeterministicSecretFromMnemonic() {
        // Arrange
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        int counter = 0;

        // Act
        DeterministicSecret secret =
                SecretFactory.createDeterministicFromMnemonic(TEST_MNEMONIC, TEST_PASSPHRASE, keysetId, counter);

        // Assert
        assertNotNull(secret);
        assertEquals(32, secret.getDerivedBytes().length);
        assertTrue(secret.hasMetadata());
        assertEquals(keysetId, secret.getKeysetId());
        assertEquals(counter, secret.getCounter());
    }

    /**
     * Ensures mnemonic-based derivation matches master-key derivation.
     */
    @Test
    void shouldMatchMnemonicAndMasterKeyDerivation() {
        // Arrange
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        int counter = 5;

        // Act
        DeterministicSecret fromMnemonic =
                SecretFactory.createDeterministicFromMnemonic(TEST_MNEMONIC, TEST_PASSPHRASE, keysetId, counter);
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        DeterministicSecret fromMasterKey = SecretFactory.createDeterministic(masterKey, keysetId, counter);

        // Assert
        assertEquals(fromMnemonic, fromMasterKey);
        assertArrayEquals(fromMnemonic.getDerivedBytes(), fromMasterKey.getDerivedBytes());
        assertEquals(fromMnemonic.toHexString(), fromMasterKey.toHexString());
    }

    /**
     * Ensures passphrases affect deterministic secret derivation.
     */
    @Test
    void shouldProduceDifferentSecretsWhenPassphraseDiffers() {
        // Arrange
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        // Act
        DeterministicSecret withPassphrase =
                SecretFactory.createDeterministicFromMnemonic(TEST_MNEMONIC, "my secret passphrase", keysetId, 0);
        DeterministicSecret withoutPassphrase =
                SecretFactory.createDeterministicFromMnemonic(TEST_MNEMONIC, "", keysetId, 0);

        // Assert
        assertNotEquals(withPassphrase, withoutPassphrase);
        assertFalse(Arrays.equals(withPassphrase.getDerivedBytes(), withoutPassphrase.getDerivedBytes()));
    }

    /**
     * Ensures null mnemonics are rejected.
     */
    @Test
    void shouldRejectNullMnemonic() {
        // Arrange
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        Executable action = () -> SecretFactory.createDeterministicFromMnemonic(null, "", keysetId, 0);

        // Act & Assert
        assertThrows(NullPointerException.class, action);
    }

    /**
     * Ensures null passphrases are rejected.
     */
    @Test
    void shouldRejectNullPassphrase() {
        // Arrange
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        Executable action =
                () -> SecretFactory.createDeterministicFromMnemonic(TEST_MNEMONIC, null, keysetId, 0);

        // Act & Assert
        assertThrows(NullPointerException.class, action);
    }

    /**
     * Ensures both secret and blinding factor are returned with metadata.
     */
    @Test
    void shouldCreateSecretAndBlindingFactor() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        int counter = 0;

        // Act
        SecretFactory.SecretAndBlindingFactor pair =
                SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, counter);

        // Assert
        assertNotNull(pair);
        assertNotNull(pair.secret());
        assertEquals(keysetId, pair.secret().getKeysetId());
        assertEquals(counter, pair.secret().getCounter());
        assertNotNull(pair.blindingFactor());
        assertEquals(32, pair.blindingFactor().length);
        assertEquals(keysetId, pair.getKeysetId());
        assertEquals(counter, pair.getCounter());
    }

    /**
     * Ensures the secret and blinding factor pair is reproducible.
     */
    @Test
    void shouldProduceSamePairForSameInputs() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        // Act
        SecretFactory.SecretAndBlindingFactor firstPair =
                SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 0);
        SecretFactory.SecretAndBlindingFactor secondPair =
                SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 0);

        // Assert
        assertEquals(firstPair.secret(), secondPair.secret());
        assertArrayEquals(firstPair.secret().getDerivedBytes(), secondPair.secret().getDerivedBytes());
        assertArrayEquals(firstPair.blindingFactor(), secondPair.blindingFactor());
    }

    /**
     * Ensures different counters produce distinct secret/blinding-factor pairs.
     */
    @Test
    void shouldProduceDifferentPairsForDifferentCounters() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        // Act
        SecretFactory.SecretAndBlindingFactor firstPair =
                SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 0);
        SecretFactory.SecretAndBlindingFactor secondPair =
                SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 1);

        // Assert
        assertNotEquals(firstPair.secret(), secondPair.secret());
        assertFalse(Arrays.equals(firstPair.secret().getDerivedBytes(), secondPair.secret().getDerivedBytes()));
        assertFalse(Arrays.equals(firstPair.blindingFactor(), secondPair.blindingFactor()));
    }

    /**
     * Ensures record accessors expose metadata without creating new objects.
     */
    @Test
    void shouldExposeMetadataThroughRecordAccessors() {
        // Arrange
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        // Act
        SecretFactory.SecretAndBlindingFactor pair =
                SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 42);

        // Assert
        assertEquals(42, pair.getCounter());
        assertEquals(keysetId, pair.getKeysetId());
        assertSame(pair.secret(), pair.secret());
        assertSame(pair.blindingFactor(), pair.blindingFactor());
    }

    /**
     * Ensures different mnemonics produce distinct deterministic secrets.
     */
    @Test
    void shouldProduceDifferentSecretsForDifferentMnemonics() {
        // Arrange
        String firstMnemonic = "abandon abandon abandon abandon abandon abandon " +
                "abandon abandon abandon abandon abandon about";
        String secondMnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong";
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        // Act
        DeterministicSecret firstSecret =
                SecretFactory.createDeterministicFromMnemonic(firstMnemonic, "", keysetId, 0);
        DeterministicSecret secondSecret =
                SecretFactory.createDeterministicFromMnemonic(secondMnemonic, "", keysetId, 0);

        // Assert
        assertNotEquals(firstSecret, secondSecret);
        assertFalse(Arrays.equals(firstSecret.getDerivedBytes(), secondSecret.getDerivedBytes()));
    }
}
