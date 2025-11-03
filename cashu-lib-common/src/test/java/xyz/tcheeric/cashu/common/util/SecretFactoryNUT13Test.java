package xyz.tcheeric.cashu.common.util;

import org.bitcoinj.crypto.DeterministicKey;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.bips.bip39.Bip39;
import xyz.tcheeric.bips.bip32.Bip32;
import xyz.tcheeric.cashu.common.DeterministicSecret;
import xyz.tcheeric.cashu.common.KeysetId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SecretFactory NUT-13 deterministic secret generation methods.
 *
 * @author NUT-13 Implementation Team
 * @since 1.0.0
 */
class SecretFactoryNUT13Test {

    // Known test vectors
    private static final String TEST_MNEMONIC =
            "abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon about";
    private static final String TEST_PASSPHRASE = "";
    private static final String TEST_KEYSET_ID = "009a1f293253e41e";

    @Test
    void testCreateDeterministic() {
        // Derive master key
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        int counter = 0;

        // Create deterministic secret
        DeterministicSecret secret = SecretFactory.createDeterministic(masterKey, keysetId, counter);

        // Verify
        assertNotNull(secret);
        assertNotNull(secret.getData());
        assertEquals(32, secret.getData().length); // Secrets are 32 bytes
        assertTrue(secret.hasMetadata());
        assertEquals(keysetId, secret.getKeysetId());
        assertEquals(counter, secret.getCounter());
        assertNotNull(secret.getDerivationPath());
    }

    @Test
    void testCreateDeterministicReproducibility() {
        // Same inputs should produce same secret
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        DeterministicSecret secret1 = SecretFactory.createDeterministic(masterKey, keysetId, 0);
        DeterministicSecret secret2 = SecretFactory.createDeterministic(masterKey, keysetId, 0);

        // Should be equal
        assertEquals(secret1, secret2);
        assertArrayEquals(secret1.getData(), secret2.getData());
        assertEquals(secret1.toHexString(), secret2.toHexString());
    }

    @Test
    void testCreateDeterministicDifferentCounters() {
        // Different counters should produce different secrets
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        DeterministicSecret secret0 = SecretFactory.createDeterministic(masterKey, keysetId, 0);
        DeterministicSecret secret1 = SecretFactory.createDeterministic(masterKey, keysetId, 1);

        // Should be different
        assertNotEquals(secret0, secret1);
        assertFalse(java.util.Arrays.equals(secret0.getData(), secret1.getData()));
    }

    @Test
    void testCreateDeterministicDifferentKeysets() {
        // Different keysets should produce different secrets
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId1 = KeysetId.fromString("009a1f293253e41e");
        KeysetId keysetId2 = KeysetId.fromString("00bf9e9d1f935a41");

        DeterministicSecret secret1 = SecretFactory.createDeterministic(masterKey, keysetId1, 0);
        DeterministicSecret secret2 = SecretFactory.createDeterministic(masterKey, keysetId2, 0);

        // Should be different
        assertNotEquals(secret1, secret2);
        assertFalse(java.util.Arrays.equals(secret1.getData(), secret2.getData()));
    }

    @Test
    void testCreateDeterministicNullMasterKey() {
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        assertThrows(NullPointerException.class, () -> {
            SecretFactory.createDeterministic(null, keysetId, 0);
        });
    }

    @Test
    void testCreateDeterministicNullKeysetId() {
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);

        assertThrows(NullPointerException.class, () -> {
            SecretFactory.createDeterministic(masterKey, null, 0);
        });
    }

    @Test
    void testCreateDeterministicBatch() {
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        int startCounter = 0;
        int count = 10;

        // Create batch
        List<DeterministicSecret> secrets = SecretFactory.createDeterministicBatch(
                masterKey, keysetId, startCounter, count
        );

        // Verify batch
        assertNotNull(secrets);
        assertEquals(count, secrets.size());

        // Verify each secret has correct counter
        for (int i = 0; i < count; i++) {
            DeterministicSecret secret = secrets.get(i);
            assertEquals(startCounter + i, secret.getCounter());
            assertEquals(keysetId, secret.getKeysetId());
            assertTrue(secret.hasMetadata());
        }

        // Verify all secrets are different
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                assertNotEquals(secrets.get(i), secrets.get(j));
            }
        }
    }

    @Test
    void testCreateDeterministicBatchLargeCount() {
        // Test with NUT-13 recommended batch size of 100
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        List<DeterministicSecret> secrets = SecretFactory.createDeterministicBatch(
                masterKey, keysetId, 0, 100
        );

        assertEquals(100, secrets.size());
        assertEquals(0, secrets.get(0).getCounter());
        assertEquals(99, secrets.get(99).getCounter());
    }

    @Test
    void testCreateDeterministicBatchZeroCount() {
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        List<DeterministicSecret> secrets = SecretFactory.createDeterministicBatch(
                masterKey, keysetId, 0, 0
        );

        assertNotNull(secrets);
        assertTrue(secrets.isEmpty());
    }

    @Test
    void testCreateDeterministicBatchNegativeCount() {
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        assertThrows(IllegalArgumentException.class, () -> {
            SecretFactory.createDeterministicBatch(masterKey, keysetId, 0, -1);
        });
    }

    @Test
    void testCreateDeterministicFromMnemonic() {
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        int counter = 0;

        // Create directly from mnemonic
        DeterministicSecret secret = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC, TEST_PASSPHRASE, keysetId, counter
        );

        // Verify
        assertNotNull(secret);
        assertNotNull(secret.getData());
        assertEquals(32, secret.getData().length);
        assertTrue(secret.hasMetadata());
        assertEquals(keysetId, secret.getKeysetId());
        assertEquals(counter, secret.getCounter());
    }

    @Test
    void testCreateDeterministicFromMnemonicMatchesMasterKeyMethod() {
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        int counter = 5;

        // Method 1: From mnemonic directly
        DeterministicSecret secret1 = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC, TEST_PASSPHRASE, keysetId, counter
        );

        // Method 2: Derive master key first
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        DeterministicSecret secret2 = SecretFactory.createDeterministic(masterKey, keysetId, counter);

        // Should produce identical secrets
        assertEquals(secret1, secret2);
        assertArrayEquals(secret1.getData(), secret2.getData());
        assertEquals(secret1.toHexString(), secret2.toHexString());
    }

    @Test
    void testCreateDeterministicFromMnemonicWithPassphrase() {
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        String passphrase = "my secret passphrase";

        DeterministicSecret secretWithPass = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC, passphrase, keysetId, 0
        );

        DeterministicSecret secretWithoutPass = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC, "", keysetId, 0
        );

        // Different passphrases should produce different secrets
        assertNotEquals(secretWithPass, secretWithoutPass);
        assertFalse(java.util.Arrays.equals(secretWithPass.getData(), secretWithoutPass.getData()));
    }

    @Test
    void testCreateDeterministicFromMnemonicNullMnemonic() {
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        assertThrows(NullPointerException.class, () -> {
            SecretFactory.createDeterministicFromMnemonic(null, "", keysetId, 0);
        });
    }

    @Test
    void testCreateDeterministicFromMnemonicNullPassphrase() {
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        assertThrows(NullPointerException.class, () -> {
            SecretFactory.createDeterministicFromMnemonic(TEST_MNEMONIC, null, keysetId, 0);
        });
    }

    @Test
    void testCreateDeterministicWithBlindingFactor() {
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);
        int counter = 0;

        // Create secret with blinding factor
        SecretFactory.SecretAndBlindingFactor pair = SecretFactory.createDeterministicWithBlindingFactor(
                masterKey, keysetId, counter
        );

        // Verify secret
        assertNotNull(pair);
        assertNotNull(pair.secret());
        assertEquals(keysetId, pair.secret().getKeysetId());
        assertEquals(counter, pair.secret().getCounter());

        // Verify blinding factor
        assertNotNull(pair.blindingFactor());
        assertEquals(32, pair.blindingFactor().length); // Blinding factors are 32 bytes

        // Verify convenience methods
        assertEquals(keysetId, pair.getKeysetId());
        assertEquals(counter, pair.getCounter());
    }

    @Test
    void testCreateDeterministicWithBlindingFactorReproducibility() {
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        var pair1 = SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 0);
        var pair2 = SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 0);

        // Both secret and blinding factor should be identical
        assertEquals(pair1.secret(), pair2.secret());
        assertArrayEquals(pair1.secret().getData(), pair2.secret().getData());
        assertArrayEquals(pair1.blindingFactor(), pair2.blindingFactor());
    }

    @Test
    void testCreateDeterministicWithBlindingFactorDifferentValues() {
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        var pair0 = SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 0);
        var pair1 = SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 1);

        // Secret and blinding factor should both be different for different counters
        assertNotEquals(pair0.secret(), pair1.secret());
        assertFalse(java.util.Arrays.equals(pair0.secret().getData(), pair1.secret().getData()));
        assertFalse(java.util.Arrays.equals(pair0.blindingFactor(), pair1.blindingFactor()));
    }

    @Test
    void testSecretAndBlindingFactorRecord() {
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC, TEST_PASSPHRASE);
        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        var pair = SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 42);

        // Test record methods
        assertEquals(42, pair.getCounter());
        assertEquals(keysetId, pair.getKeysetId());
        assertSame(pair.secret(), pair.secret()); // record accessor
        assertSame(pair.blindingFactor(), pair.blindingFactor()); // record accessor
    }

    @Test
    void testMultipleMnemonicsProduceDifferentSecrets() {
        String mnemonic1 = "abandon abandon abandon abandon abandon abandon " +
                          "abandon abandon abandon abandon abandon about";
        String mnemonic2 = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong";

        KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID);

        DeterministicSecret secret1 = SecretFactory.createDeterministicFromMnemonic(
                mnemonic1, "", keysetId, 0
        );

        DeterministicSecret secret2 = SecretFactory.createDeterministicFromMnemonic(
                mnemonic2, "", keysetId, 0
        );

        // Different mnemonics should produce different secrets
        assertNotEquals(secret1, secret2);
        assertFalse(java.util.Arrays.equals(secret1.getData(), secret2.getData()));
    }
}
