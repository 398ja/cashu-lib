package xyz.tcheeric.cashu.vectors;

import lombok.Value;
import org.bitcoinj.crypto.DeterministicKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.tcheeric.bips.bip32.nut.Nut13Derivation;
import xyz.tcheeric.bips.bip39.Bip39;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.util.SecretFactory;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NUT-13 vectors: deterministic secrets and blinding factors derived from a mnemonic.
 *
 * <p>Only the version 1 derivation is exercised. Version 2 keyset ids and the P2PK derivation are
 * not implemented (issues #247 and #248) and their vectors are recorded as uncovered in
 * {@code docs/reference/nut-test-vector-coverage.md}.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/tests/13-tests.md">NUT-13 test vectors</a>
 */
class Nut13VectorTest {

    private static final VectorDocument VECTORS = VectorDocument.load("13-tests.md");

    private static final int KEYSET_ID_BLOCK = 0;
    private static final int MNEMONIC_BLOCK = 1;
    private static final int SECRET_BLOCK = 2;
    private static final int BLINDING_FACTOR_BLOCK = 3;
    private static final int DERIVATION_PATH_BLOCK = 4;
    private static final int COUNTER_COUNT = 5;

    private static final String EMPTY_PASSPHRASE = "";

    static Stream<CounterVector> counterVectors() {
        Map<String, String> secrets = jsonFields(SECRET_BLOCK);
        Map<String, String> blindingFactors = jsonFields(BLINDING_FACTOR_BLOCK);
        Map<String, String> derivationPaths = jsonFields(DERIVATION_PATH_BLOCK);
        return IntStream.range(0, COUNTER_COUNT)
                .mapToObj(counter -> new CounterVector(
                        counter,
                        secrets.get("secret_" + counter),
                        blindingFactors.get("r_" + counter),
                        derivationPaths.get("derivation_path_" + counter)));
    }

    /**
     * Ensures a version 1 keyset id converts to the published integer used in the derivation path.
     */
    @Test
    void shouldProducePublishedIntegerWhenConvertingVersionOneKeysetId() {
        // Arrange
        Map<String, String> vector = jsonFields(KEYSET_ID_BLOCK);
        KeysetId keysetId = KeysetId.fromString(vector.get("keyset_id"));

        // Act
        int keysetIdInt = keysetId.toInt();

        // Assert
        assertThat(keysetIdInt).isEqualTo(Integer.parseInt(vector.get("keyest_id_int")));
    }

    /**
     * Ensures each counter derives the published secret from the vector mnemonic.
     */
    @ParameterizedTest(name = "counter {0}")
    @MethodSource("counterVectors")
    void shouldDerivePublishedSecretWhenCounterIsGiven(CounterVector vector) {
        // Arrange
        DeterministicKey masterKey = masterKey();

        // Act
        byte[] secret = SecretFactory.createDeterministic(masterKey, keysetId(), vector.getCounter()).getDerivedBytes();

        // Assert
        assertThat(Utils.bytesToHexString(secret)).isEqualTo(vector.getSecret());
    }

    /**
     * Ensures each counter derives the published blinding factor from the vector mnemonic.
     */
    @ParameterizedTest(name = "counter {0}")
    @MethodSource("counterVectors")
    void shouldDerivePublishedBlindingFactorWhenCounterIsGiven(CounterVector vector) {
        // Arrange
        DeterministicKey masterKey = masterKey();

        // Act
        byte[] blindingFactor =
                Nut13Derivation.deriveBlindingFactor(masterKey, keysetId().toString(), vector.getCounter());

        // Assert
        assertThat(Utils.bytesToHexString(blindingFactor)).isEqualTo(vector.getBlindingFactor());
    }

    /**
     * Ensures the secret derivation path matches the published path for each counter.
     */
    @ParameterizedTest(name = "counter {0}")
    @MethodSource("counterVectors")
    void shouldBuildPublishedDerivationPathWhenCounterIsGiven(CounterVector vector) {
        // Act
        String derivationPath =
                Nut13Derivation.buildSecretDerivationPath(keysetId().toInt(), vector.getCounter());

        // Assert
        assertThat(derivationPath).startsWith(vector.getDerivationPath());
    }

    private static KeysetId keysetId() {
        return KeysetId.fromString(jsonFields(KEYSET_ID_BLOCK).get("keyset_id"));
    }

    private static DeterministicKey masterKey() {
        return Bip39.mnemonicToMasterKey(jsonFields(MNEMONIC_BLOCK).get("mnemonic"), EMPTY_PASSPHRASE);
    }

    private static Map<String, String> jsonFields(int blockIndex) {
        return JsonVectors.flatFields(VECTORS.block("json", blockIndex).text());
    }

    /**
     * The published secret, blinding factor and derivation path for one counter.
     */
    @Value
    static class CounterVector {

        int counter;
        String secret;
        String blindingFactor;
        String derivationPath;

        @Override
        public String toString() {
            return String.valueOf(counter);
        }
    }
}
