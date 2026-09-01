package xyz.tcheeric.cashu.common.nut13;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.KeysetIdVersion;
import xyz.tcheeric.cashu.common.util.SecretFactory;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that NUT-13 deterministic derivation dispatches on the keyset id version byte and
 * refuses versions it cannot derive, instead of silently recovering nothing.
 */
class DeterministicDerivationVersionDispatchTest {

    private static final String V1_KEYSET_ID = "009a1f293253e41e";
    private static final String V2_KEYSET_ID =
            "01" + "a".repeat(KeysetIdVersion.V2.getHexLength() - 2);
    private static final String MNEMONIC =
            "half depart obvious quality work element tank gorilla view sugar picture humble";
    private static final int COUNTER = 0;

    private static DeterministicKey masterKey() {
        return HDKeyDerivation.createMasterPrivateKey("cashu-lib-test-seed".getBytes(StandardCharsets.UTF_8));
    }

    /** A version 1 keyset id resolves to version 1. */
    @Test
    void shouldResolveVersionOneWhenIdCarriesZeroVersionByte() {
        KeysetId keysetId = KeysetId.fromString(V1_KEYSET_ID);

        KeysetIdVersion version = keysetId.getVersion();

        assertThat(version).isEqualTo(KeysetIdVersion.V1);
    }

    /** A 66 character id starting with 0x01 resolves to version 2. */
    @Test
    void shouldResolveVersionTwoWhenIdCarriesOneVersionByte() {
        KeysetId keysetId = KeysetId.fromString(V2_KEYSET_ID);

        KeysetIdVersion version = keysetId.getVersion();

        assertThat(version).isEqualTo(KeysetIdVersion.V2);
    }

    /** Ids of an unrecognised length are still rejected outright. */
    @Test
    void shouldRejectIdWhenLengthMatchesNoKnownVersion() {
        assertThatThrownBy(() -> KeysetId.fromString("00ab"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid keyset id: 00ab");
    }

    /** Deriving a secret for a version 1 keyset keeps working. */
    @Test
    void shouldDeriveSecretWhenKeysetIdIsVersionOne() {
        KeysetId keysetId = KeysetId.fromString(V1_KEYSET_ID);

        assertThatCode(() -> SecretFactory.createDeterministic(masterKey(), keysetId, COUNTER))
                .doesNotThrowAnyException();
    }

    /** Deriving a secret for a version 2 keyset fails loudly rather than returning v1 secrets. */
    @Test
    void shouldThrowWhenDerivingSecretForVersionTwoKeyset() {
        KeysetId keysetId = KeysetId.fromString(V2_KEYSET_ID);

        assertThatThrownBy(() -> SecretFactory.createDeterministic(masterKey(), keysetId, COUNTER))
                .isInstanceOf(UnsupportedKeysetVersionException.class)
                .hasMessageContaining(V2_KEYSET_ID)
                .hasMessageContaining("V2");
    }

    /** The exception carries the offending keyset id and version for callers to act on. */
    @Test
    void shouldCarryKeysetContextWhenDerivationIsRefused() {
        KeysetId keysetId = KeysetId.fromString(V2_KEYSET_ID);

        UnsupportedKeysetVersionException exception = new UnsupportedKeysetVersionException(keysetId, KeysetIdVersion.V2);

        assertThat(exception.getKeysetId()).isEqualTo(keysetId);
        assertThat(exception.getVersion()).isEqualTo(KeysetIdVersion.V2);
    }

    /** Batch derivation refuses a version 2 keyset before generating any secret. */
    @Test
    void shouldThrowWhenDerivingBatchForVersionTwoKeyset() {
        KeysetId keysetId = KeysetId.fromString(V2_KEYSET_ID);

        assertThatThrownBy(() -> SecretFactory.createDeterministicBatch(masterKey(), keysetId, COUNTER, 3))
                .isInstanceOf(UnsupportedKeysetVersionException.class);
    }

    /** Mnemonic based derivation refuses a version 2 keyset. */
    @Test
    void shouldThrowWhenDerivingFromMnemonicForVersionTwoKeyset() {
        KeysetId keysetId = KeysetId.fromString(V2_KEYSET_ID);

        assertThatThrownBy(() -> SecretFactory.createDeterministicFromMnemonic(MNEMONIC, "", keysetId, COUNTER))
                .isInstanceOf(UnsupportedKeysetVersionException.class);
    }

    /** Deriving a secret and blinding factor together refuses a version 2 keyset. */
    @Test
    void shouldThrowWhenDerivingBlindingFactorForVersionTwoKeyset() {
        KeysetId keysetId = KeysetId.fromString(V2_KEYSET_ID);

        assertThatThrownBy(() -> SecretFactory.createDeterministicWithBlindingFactor(masterKey(), keysetId, COUNTER))
                .isInstanceOf(UnsupportedKeysetVersionException.class);
    }
}
