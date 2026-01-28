package xyz.tcheeric.cashu.protocol;

import org.bitcoinj.crypto.DeterministicKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.bips.bip32.nut.Nut13Derivation;
import xyz.tcheeric.bips.bip39.Bip39;
import xyz.tcheeric.cashu.common.*;
import xyz.tcheeric.cashu.common.nut13.DeterministicSecret;
import xyz.tcheeric.cashu.common.util.SecretFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for NUT-13 deterministic secret derivation.
 *
 * <p>This test suite verifies the complete NUT-13 implementation across:
 * <ul>
 *   <li>BIP39 mnemonic to BIP32 master key derivation</li>
 *   <li>NUT-13 derivation path formatting and parsing</li>
 *   <li>Keyset ID to integer conversion (mod 2^31 - 1)</li>
 *   <li>Deterministic secret and blinding factor derivation</li>
 *   <li>Secret reproducibility from the same mnemonic</li>
 *   <li>Integration with bip-utils library</li>
 * </ul>
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/13.md">NUT-13 Specification</a>
 * @author NUT-13 Implementation Team
 */
@DisplayName("NUT-13 Integration Tests")
class NUT13IntegrationTest {

    // Test vectors from BIP39 specification
    private static final String TEST_MNEMONIC_12_WORDS =
            "abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon about";

    private static final String TEST_MNEMONIC_24_WORDS =
            "abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon art";

    private static final String CUSTOM_TEST_MNEMONIC =
            "legal winner thank year wave sausage worth useful legal winner thank yellow";

    private static final String TEST_KEYSET_ID_1 = "009a1f293253e41e";
    private static final String TEST_KEYSET_ID_2 = "00bf9e9d1f935a41";
    private static final String TEST_KEYSET_ID_3 = "00ad268c4d1f5826";

    // =========================== Derivation Path Tests ===========================

    @Nested
    @DisplayName("Derivation Path Formatting")
    class DerivationPathFormattingTests {

        @Test
        @DisplayName("Should format secret derivation path correctly")
        void shouldFormatSecretDerivationPath() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);
            SecretDerivationPath path = new SecretDerivationPath();
            path.setKeysetId(keysetId);
            path.setCounter(0);

            // Act
            String formattedPath = path.toString();

            // Assert
            assertNotNull(formattedPath);
            assertTrue(formattedPath.startsWith("m/129372'/0'/"));
            assertTrue(formattedPath.endsWith("'/0'/0")); // suffix=0 for secrets
            assertEquals(0, path.getSuffix());
        }

        @Test
        @DisplayName("Should format blinding factor derivation path correctly")
        void shouldFormatBlindingFactorDerivationPath() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);
            RDerivationPath path = new RDerivationPath();
            path.setKeysetId(keysetId);
            path.setCounter(0);

            // Act
            String formattedPath = path.toString();

            // Assert
            assertNotNull(formattedPath);
            assertTrue(formattedPath.startsWith("m/129372'/0'/"));
            assertTrue(formattedPath.endsWith("'/0'/1")); // suffix=1 for blinding factors
            assertEquals(1, path.getSuffix());
        }

        @Test
        @DisplayName("Should include NUT-13 purpose constant in path")
        void shouldIncludeNut13Purpose() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);
            SecretDerivationPath path = new SecretDerivationPath();
            path.setKeysetId(keysetId);
            path.setCounter(0);

            // Act
            String formattedPath = path.toString();

            // Assert
            assertTrue(formattedPath.contains("129372'")); // 🥜 peanut emoji UTF-8
        }

        @Test
        @DisplayName("Should format different counters correctly")
        void shouldFormatDifferentCounters() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);
            SecretDerivationPath path = new SecretDerivationPath();
            path.setKeysetId(keysetId);

            // Act & Assert
            path.setCounter(0);
            assertTrue(path.toString().contains("'/0'/0"));

            path.setCounter(1);
            assertTrue(path.toString().contains("'/1'/0"));

            path.setCounter(99);
            assertTrue(path.toString().contains("'/99'/0"));

            path.setCounter(1000);
            assertTrue(path.toString().contains("'/1000'/0"));
        }

        @Test
        @DisplayName("Should use keyset integer representation in path")
        void shouldUseKeysetIntegerInPath() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);
            int expectedInt = keysetId.toInt();
            SecretDerivationPath path = new SecretDerivationPath();
            path.setKeysetId(keysetId);
            path.setCounter(0);

            // Act
            String formattedPath = path.toString();

            // Assert
            assertTrue(formattedPath.contains("/" + expectedInt + "'"));
        }
    }

    // =========================== Keyset ID Conversion Tests ===========================

    @Nested
    @DisplayName("Keyset ID to Integer Conversion")
    class KeysetIdConversionTests {

        @Test
        @DisplayName("Should convert keyset ID to integer mod 2^31-1")
        void shouldConvertKeysetIdToInteger() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            int result = keysetId.toInt();

            // Assert
            assertTrue(result >= 0);
            assertTrue(result < Integer.MAX_VALUE); // Less than 2^31 - 1
        }

        @Test
        @DisplayName("Should produce consistent integer for same keyset ID")
        void shouldProduceConsistentInteger() {
            // Arrange
            KeysetId keysetId1 = KeysetId.fromString(TEST_KEYSET_ID_1);
            KeysetId keysetId2 = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            int result1 = keysetId1.toInt();
            int result2 = keysetId2.toInt();

            // Assert
            assertEquals(result1, result2);
        }

        @Test
        @DisplayName("Should produce different integers for different keyset IDs")
        void shouldProduceDifferentIntegersForDifferentKeysets() {
            // Arrange
            KeysetId keysetId1 = KeysetId.fromString(TEST_KEYSET_ID_1);
            KeysetId keysetId2 = KeysetId.fromString(TEST_KEYSET_ID_2);
            KeysetId keysetId3 = KeysetId.fromString(TEST_KEYSET_ID_3);

            // Act
            int result1 = keysetId1.toInt();
            int result2 = keysetId2.toInt();
            int result3 = keysetId3.toInt();

            // Assert
            assertNotEquals(result1, result2);
            assertNotEquals(result1, result3);
            assertNotEquals(result2, result3);
        }

        @Test
        @DisplayName("Should handle large keyset IDs correctly")
        void shouldHandleLargeKeysetIds() {
            // Arrange - maximum value keyset ID
            KeysetId keysetId = KeysetId.fromString("ffffffffffffffff");

            // Act
            int result = keysetId.toInt();

            // Assert
            assertTrue(result >= 0);
            assertTrue(result < Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("Should be compatible with bip-utils Nut13Derivation")
        void shouldBeCompatibleWithBipUtils() {
            // Arrange
            String keysetIdHex = TEST_KEYSET_ID_1;
            KeysetId keysetId = KeysetId.fromString(keysetIdHex);

            // Act
            int ourImplementation = keysetId.toInt();
            int bipUtilsImplementation = Nut13Derivation.keysetIdToInt(keysetIdHex);

            // Assert - both implementations should produce the same result
            assertEquals(bipUtilsImplementation, ourImplementation,
                "KeysetId.toInt() must match Nut13Derivation.keysetIdToInt()");
        }
    }

    // =========================== Secret Derivation Tests ===========================

    @Nested
    @DisplayName("Deterministic Secret Derivation")
    class SecretDerivationTests {

        @Test
        @DisplayName("Should derive secret from mnemonic")
        void shouldDeriveSecretFromMnemonic() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            DeterministicSecret secret = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC_12_WORDS,
                "",
                keysetId,
                0
            );

            // Assert
            assertNotNull(secret);
            assertEquals(32, secret.getData().length);
            assertTrue(secret.hasMetadata());
            assertEquals(keysetId, secret.getKeysetId());
            assertEquals(0, secret.getCounter());
        }

        @Test
        @DisplayName("Should derive blinding factor from mnemonic")
        void shouldDeriveBlindingFactorFromMnemonic() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            byte[] blindingFactor = Nut13Derivation.deriveBlindingFactor(
                masterKey,
                keysetId.toString(),
                0
            );

            // Assert
            assertNotNull(blindingFactor);
            assertEquals(32, blindingFactor.length);
        }

        @Test
        @DisplayName("Should derive both secret and blinding factor together")
        void shouldDeriveBothSecretAndBlindingFactor() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            SecretFactory.SecretAndBlindingFactor pair =
                SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 0);

            // Assert
            assertNotNull(pair);
            assertNotNull(pair.secret());
            assertNotNull(pair.blindingFactor());
            assertEquals(32, pair.secret().getData().length);
            assertEquals(32, pair.blindingFactor().length);
            assertEquals(keysetId, pair.getKeysetId());
            assertEquals(0, pair.getCounter());
        }

        @Test
        @DisplayName("Should derive different secrets for different counters")
        void shouldDeriveDifferentSecretsForDifferentCounters() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            DeterministicSecret secret0 = SecretFactory.createDeterministic(masterKey, keysetId, 0);
            DeterministicSecret secret1 = SecretFactory.createDeterministic(masterKey, keysetId, 1);
            DeterministicSecret secret99 = SecretFactory.createDeterministic(masterKey, keysetId, 99);

            // Assert
            assertNotEquals(secret0, secret1);
            assertNotEquals(secret0, secret99);
            assertNotEquals(secret1, secret99);
            assertFalse(Arrays.equals(secret0.getData(), secret1.getData()));
            assertFalse(Arrays.equals(secret0.getData(), secret99.getData()));
            assertFalse(Arrays.equals(secret1.getData(), secret99.getData()));
        }

        @Test
        @DisplayName("Should derive different secrets for different keysets")
        void shouldDeriveDifferentSecretsForDifferentKeysets() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keyset1 = KeysetId.fromString(TEST_KEYSET_ID_1);
            KeysetId keyset2 = KeysetId.fromString(TEST_KEYSET_ID_2);

            // Act
            DeterministicSecret secret1 = SecretFactory.createDeterministic(masterKey, keyset1, 0);
            DeterministicSecret secret2 = SecretFactory.createDeterministic(masterKey, keyset2, 0);

            // Assert
            assertNotEquals(secret1, secret2);
            assertFalse(Arrays.equals(secret1.getData(), secret2.getData()));
        }

        @Test
        @DisplayName("Should derive 32-byte secrets")
        void shouldDerive32ByteSecrets() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            DeterministicSecret secret = SecretFactory.createDeterministic(masterKey, keysetId, 0);

            // Assert
            assertEquals(32, secret.getData().length);
        }
    }

    // =========================== Reproducibility Tests ===========================

    @Nested
    @DisplayName("Secret Reproducibility")
    class SecretReproducibilityTests {

        @Test
        @DisplayName("Should produce identical secrets from same mnemonic")
        void shouldProduceIdenticalSecretsFromSameMnemonic() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);
            String mnemonic = TEST_MNEMONIC_12_WORDS;

            // Act - derive twice from same mnemonic
            DeterministicSecret secret1 = SecretFactory.createDeterministicFromMnemonic(
                mnemonic, "", keysetId, 0);
            DeterministicSecret secret2 = SecretFactory.createDeterministicFromMnemonic(
                mnemonic, "", keysetId, 0);

            // Assert
            assertEquals(secret1, secret2);
            assertArrayEquals(secret1.getData(), secret2.getData());
            assertEquals(secret1.toHexString(), secret2.toHexString());
        }

        @Test
        @DisplayName("Should produce identical secrets from same master key")
        void shouldProduceIdenticalSecretsFromSameMasterKey() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act - derive twice from same master key
            DeterministicSecret secret1 = SecretFactory.createDeterministic(masterKey, keysetId, 5);
            DeterministicSecret secret2 = SecretFactory.createDeterministic(masterKey, keysetId, 5);

            // Assert
            assertEquals(secret1, secret2);
            assertArrayEquals(secret1.getData(), secret2.getData());
        }

        @Test
        @DisplayName("Should produce identical blinding factors from same inputs")
        void shouldProduceIdenticalBlindingFactors() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            SecretFactory.SecretAndBlindingFactor pair1 =
                SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 0);
            SecretFactory.SecretAndBlindingFactor pair2 =
                SecretFactory.createDeterministicWithBlindingFactor(masterKey, keysetId, 0);

            // Assert
            assertEquals(pair1.secret(), pair2.secret());
            assertArrayEquals(pair1.blindingFactor(), pair2.blindingFactor());
        }

        @Test
        @DisplayName("Should recover secrets across multiple derivations")
        void shouldRecoverSecretsAcrossMultipleDerivations() {
            // Arrange
            String mnemonic = TEST_MNEMONIC_12_WORDS;
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act - simulate initial wallet creation
            DeterministicKey initialMasterKey = Bip39.mnemonicToMasterKey(mnemonic, "");
            DeterministicSecret initialSecret0 = SecretFactory.createDeterministic(initialMasterKey, keysetId, 0);
            DeterministicSecret initialSecret1 = SecretFactory.createDeterministic(initialMasterKey, keysetId, 1);
            DeterministicSecret initialSecret99 = SecretFactory.createDeterministic(initialMasterKey, keysetId, 99);

            // Simulate wallet recovery from same mnemonic
            DeterministicKey recoveredMasterKey = Bip39.mnemonicToMasterKey(mnemonic, "");
            DeterministicSecret recoveredSecret0 = SecretFactory.createDeterministic(recoveredMasterKey, keysetId, 0);
            DeterministicSecret recoveredSecret1 = SecretFactory.createDeterministic(recoveredMasterKey, keysetId, 1);
            DeterministicSecret recoveredSecret99 = SecretFactory.createDeterministic(recoveredMasterKey, keysetId, 99);

            // Assert - all secrets should match
            assertEquals(initialSecret0, recoveredSecret0);
            assertEquals(initialSecret1, recoveredSecret1);
            assertEquals(initialSecret99, recoveredSecret99);
            assertArrayEquals(initialSecret0.getData(), recoveredSecret0.getData());
            assertArrayEquals(initialSecret1.getData(), recoveredSecret1.getData());
            assertArrayEquals(initialSecret99.getData(), recoveredSecret99.getData());
        }

        @Test
        @DisplayName("Should produce different secrets with passphrase")
        void shouldProduceDifferentSecretsWithPassphrase() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            DeterministicSecret withoutPassphrase = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC_12_WORDS, "", keysetId, 0);
            DeterministicSecret withPassphrase = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC_12_WORDS, "my secret passphrase", keysetId, 0);

            // Assert
            assertNotEquals(withoutPassphrase, withPassphrase);
            assertFalse(Arrays.equals(withoutPassphrase.getData(), withPassphrase.getData()));
        }

        @Test
        @DisplayName("Should produce consistent results with 24-word mnemonic")
        void shouldProduceConsistentResultsWith24Words() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            DeterministicSecret secret1 = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC_24_WORDS, "", keysetId, 0);
            DeterministicSecret secret2 = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC_24_WORDS, "", keysetId, 0);

            // Assert
            assertEquals(secret1, secret2);
            assertArrayEquals(secret1.getData(), secret2.getData());
        }
    }

    // =========================== Batch Operations Tests ===========================

    @Nested
    @DisplayName("Batch Secret Generation")
    class BatchSecretGenerationTests {

        @Test
        @DisplayName("Should generate batch of 100 secrets")
        void shouldGenerateBatchOf100Secrets() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            List<DeterministicSecret> secrets =
                SecretFactory.createDeterministicBatch(masterKey, keysetId, 0, 100);

            // Assert
            assertEquals(100, secrets.size());
            for (int i = 0; i < 100; i++) {
                assertEquals(i, secrets.get(i).getCounter());
                assertEquals(keysetId, secrets.get(i).getKeysetId());
            }
        }

        @Test
        @DisplayName("Should generate batch starting from specific counter")
        void shouldGenerateBatchFromSpecificCounter() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            List<DeterministicSecret> batch = SecretFactory.createDeterministicBatch(
                masterKey, keysetId, 100, 50);

            // Assert
            assertEquals(50, batch.size());
            assertEquals(100, batch.get(0).getCounter());
            assertEquals(149, batch.get(49).getCounter());
        }

        @Test
        @DisplayName("Should produce all unique secrets in batch")
        void shouldProduceAllUniqueSecretsInBatch() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            List<DeterministicSecret> batch = SecretFactory.createDeterministicBatch(
                masterKey, keysetId, 0, 100);

            // Assert - verify all secrets are unique
            for (int i = 0; i < batch.size(); i++) {
                for (int j = i + 1; j < batch.size(); j++) {
                    assertNotEquals(batch.get(i), batch.get(j),
                        "Secrets at positions " + i + " and " + j + " should be different");
                }
            }
        }

        @Test
        @DisplayName("Should match individual derivation")
        void shouldMatchIndividualDerivation() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            List<DeterministicSecret> batch = SecretFactory.createDeterministicBatch(
                masterKey, keysetId, 0, 10);
            DeterministicSecret individual5 = SecretFactory.createDeterministic(masterKey, keysetId, 5);

            // Assert
            assertEquals(individual5, batch.get(5));
            assertArrayEquals(individual5.getData(), batch.get(5).getData());
        }
    }

    // =========================== Integration with bip-utils Tests ===========================

    @Nested
    @DisplayName("Integration with bip-utils")
    class BipUtilsIntegrationTests {

        @Test
        @DisplayName("Should use Nut13Derivation for secret generation")
        void shouldUseNut13DerivationForSecretGeneration() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act - use bip-utils directly
            byte[] bipUtilsSecret = Nut13Derivation.deriveSecret(
                masterKey, keysetId.toString(), 0);

            // Use SecretFactory (which wraps bip-utils)
            DeterministicSecret factorySecret = SecretFactory.createDeterministic(
                masterKey, keysetId, 0);

            // Assert - both should produce identical results
            assertArrayEquals(bipUtilsSecret, factorySecret.getData());
        }

        @Test
        @DisplayName("Should use Bip39 for mnemonic to master key conversion")
        void shouldUseBip39ForMnemonicConversion() {
            // Arrange
            String mnemonic = TEST_MNEMONIC_12_WORDS;

            // Act
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(mnemonic, "");

            // Assert
            assertNotNull(masterKey);
            assertNotNull(masterKey.getPrivKeyBytes());
            assertEquals(32, masterKey.getPrivKeyBytes().length);
        }

        @Test
        @DisplayName("Should validate mnemonics using Bip39")
        void shouldValidateMnemonicsUsingBip39() {
            // Act & Assert
            assertTrue(Bip39.isValidMnemonic(TEST_MNEMONIC_12_WORDS));
            assertTrue(Bip39.isValidMnemonic(TEST_MNEMONIC_24_WORDS));
            assertTrue(Bip39.isValidMnemonic(CUSTOM_TEST_MNEMONIC));
            assertFalse(Bip39.isValidMnemonic("invalid mnemonic phrase"));
            assertFalse(Bip39.isValidMnemonic("abandon abandon abandon"));
        }

        @Test
        @DisplayName("Should use NUT-13 constants from bip-utils")
        void shouldUseNut13ConstantsFromBipUtils() {
            // Assert - verify constants match
            assertEquals(129372, Nut13Derivation.NUT13_PURPOSE);
            assertEquals(0, Nut13Derivation.COIN_TYPE);
            assertEquals(0, Nut13Derivation.INDEX_SECRET);
            assertEquals(1, Nut13Derivation.INDEX_BLINDING_FACTOR);
        }

        @Test
        @DisplayName("Should match bip-utils derivation path format")
        void shouldMatchBipUtilsDerivationPathFormat() {
            // Arrange
            String keysetIdHex = TEST_KEYSET_ID_1;
            int counter = 42;
            int keysetIdInt = Nut13Derivation.keysetIdToInt(keysetIdHex);

            // Act
            String bipUtilsPath = Nut13Derivation.buildSecretDerivationPath(keysetIdInt, counter);

            KeysetId keysetId = KeysetId.fromString(keysetIdHex);
            SecretDerivationPath ourPath = new SecretDerivationPath();
            ourPath.setKeysetId(keysetId);
            ourPath.setCounter(counter);
            String ourPathStr = ourPath.toString();

            // Assert
            assertEquals(bipUtilsPath, ourPathStr);
        }
    }

    // =========================== DeterministicSecret Serialization Tests ===========================

    @Nested
    @DisplayName("DeterministicSecret Serialization")
    class DeterministicSecretSerializationTests {

        @Test
        @DisplayName("Should serialize to hex string")
        void shouldSerializeToHexString() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);
            DeterministicSecret secret = SecretFactory.createDeterministic(masterKey, keysetId, 0);

            // Act
            String hexString = secret.toHexString();

            // Assert
            assertNotNull(hexString);
            assertEquals(64, hexString.length()); // 32 bytes = 64 hex characters
            assertTrue(hexString.matches("^[0-9a-f]{64}$"));
        }

        @Test
        @DisplayName("Should deserialize from hex string")
        void shouldDeserializeFromHexString() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);
            DeterministicSecret original = SecretFactory.createDeterministic(masterKey, keysetId, 0);
            String hexString = original.toHexString();

            // Act
            DeterministicSecret deserialized = DeterministicSecret.fromString(hexString);

            // Assert
            assertEquals(original, deserialized);
            assertArrayEquals(original.getData(), deserialized.getData());
        }

        @Test
        @DisplayName("Should round-trip through hex serialization")
        void shouldRoundTripThroughHexSerialization() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);
            DeterministicSecret original = SecretFactory.createDeterministic(masterKey, keysetId, 42);

            // Act
            String hex = original.toHexString();
            DeterministicSecret roundTripped = DeterministicSecret.fromString(hex);

            // Assert
            assertEquals(original, roundTripped);
            assertArrayEquals(original.getData(), roundTripped.getData());
            // Note: metadata is lost during serialization (as expected)
            assertFalse(roundTripped.hasMetadata());
        }
    }

    // =========================== Edge Cases and Error Handling ===========================

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle counter value of zero")
        void shouldHandleCounterZero() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            DeterministicSecret secret = SecretFactory.createDeterministic(masterKey, keysetId, 0);

            // Assert
            assertEquals(0, secret.getCounter());
            assertNotNull(secret.getData());
        }

        @Test
        @DisplayName("Should handle large counter values")
        void shouldHandleLargeCounterValues() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            DeterministicSecret secret1000 = SecretFactory.createDeterministic(masterKey, keysetId, 1000);
            DeterministicSecret secret10000 = SecretFactory.createDeterministic(masterKey, keysetId, 10000);

            // Assert
            assertEquals(1000, secret1000.getCounter());
            assertEquals(10000, secret10000.getCounter());
            assertNotEquals(secret1000, secret10000);
        }

        @Test
        @DisplayName("Should reject null master key")
        void shouldRejectNullMasterKey() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act & Assert
            assertThrows(NullPointerException.class, () ->
                SecretFactory.createDeterministic(null, keysetId, 0));
        }

        @Test
        @DisplayName("Should reject null keyset ID")
        void shouldRejectNullKeysetId() {
            // Arrange
            DeterministicKey masterKey = Bip39.mnemonicToMasterKey(TEST_MNEMONIC_12_WORDS, "");

            // Act & Assert
            assertThrows(NullPointerException.class, () ->
                SecretFactory.createDeterministic(masterKey, null, 0));
        }

        @Test
        @DisplayName("Should handle empty passphrase")
        void shouldHandleEmptyPassphrase() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            DeterministicSecret withEmptyString = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC_12_WORDS, "", keysetId, 0);
            DeterministicSecret withExplicitEmpty = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC_12_WORDS, "", keysetId, 0);

            // Assert
            assertEquals(withEmptyString, withExplicitEmpty);
            assertArrayEquals(withEmptyString.getData(), withExplicitEmpty.getData());
        }

        @Test
        @DisplayName("Should produce different results for different mnemonics")
        void shouldProduceDifferentResultsForDifferentMnemonics() {
            // Arrange
            KeysetId keysetId = KeysetId.fromString(TEST_KEYSET_ID_1);

            // Act
            DeterministicSecret from12Words = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC_12_WORDS, "", keysetId, 0);
            DeterministicSecret from24Words = SecretFactory.createDeterministicFromMnemonic(
                TEST_MNEMONIC_24_WORDS, "", keysetId, 0);
            DeterministicSecret fromCustom = SecretFactory.createDeterministicFromMnemonic(
                CUSTOM_TEST_MNEMONIC, "", keysetId, 0);

            // Assert
            assertNotEquals(from12Words, from24Words);
            assertNotEquals(from12Words, fromCustom);
            assertNotEquals(from24Words, fromCustom);
        }
    }
}
