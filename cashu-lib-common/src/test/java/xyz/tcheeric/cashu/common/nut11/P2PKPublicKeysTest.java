package xyz.tcheeric.cashu.common.nut11;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NUT-11 requires compressed secp256k1 public keys, and requires that keys be compared on their
 * lowercase x-coordinate with the parity prefix ignored.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/11.md">NUT-11</a>
 */
class P2PKPublicKeysTest {

    /** The secp256k1 generator point, even-y. A known-valid compressed key. */
    private static final String VALID_02 =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    /** Same x-coordinate as {@link #VALID_02}, odd-y. Also a valid point. */
    private static final String VALID_03 =
            "0379be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    /** The x-coordinate alone — 64 chars. Nostr's encoding, which NUT-11 does not permit. */
    private static final String X_ONLY =
            "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    /** x == p, the field prime: syntactically well-formed, not a field element. */
    private static final String X_OUT_OF_FIELD =
            "02fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f";

    @Nested
    @DisplayName("requireValid")
    class RequireValid {

        @Test
        @DisplayName("accepts a compressed key with an 02 prefix")
        void acceptsEvenParity() {
            assertThat(P2PKPublicKeys.requireValid(VALID_02, "data").toString()).isEqualTo(VALID_02);
        }

        @Test
        @DisplayName("accepts a compressed key with an 03 prefix")
        void acceptsOddParity() {
            assertThat(P2PKPublicKeys.requireValid(VALID_03, "data").toString()).isEqualTo(VALID_03);
        }

        @Test
        @DisplayName("normalises uppercase hex rather than rejecting it")
        void acceptsUppercase() {
            // NUT-11 lists uppercase and mixed case as valid, equivalent encodings.
            assertThat(P2PKPublicKeys.requireValid(VALID_02.toUpperCase(), "data").toString())
                    .isEqualTo(VALID_02);
        }

        @Test
        @DisplayName("rejects an x-only 32-byte key")
        void rejectsXOnly() {
            assertThatThrownBy(() -> P2PKPublicKeys.requireValid(X_ONLY, "data"))
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("data");
        }

        @Test
        @DisplayName("rejects an uncompressed x||y key that PublicKey.fromString would accept")
        void rejectsUncompressed() {
            // 128 hex chars. PublicKey.fromString accepts this; NUT-11 does not.
            String uncompressed = X_ONLY + X_ONLY;
            assertThatThrownBy(() -> P2PKPublicKeys.requireValid(uncompressed, "data"))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @ParameterizedTest(name = "rejects prefix {0}")
        @ValueSource(strings = {"04", "05", "00", "ff"})
        @DisplayName("rejects a 33-byte key whose prefix is not 02 or 03")
        void rejectsBadPrefix(String prefix) {
            assertThatThrownBy(() -> P2PKPublicKeys.requireValid(prefix + X_ONLY, "data"))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("rejects an x-coordinate outside the field")
        void rejectsXOutsideField() {
            assertThatThrownBy(() -> P2PKPublicKeys.requireValid(X_OUT_OF_FIELD, "pubkeys[1]"))
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("pubkeys[1]");
        }

        @Test
        @DisplayName("rejects non-hex characters")
        void rejectsNonHex() {
            String nonHex = "02zzbe667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
            assertThatThrownBy(() -> P2PKPublicKeys.requireValid(nonHex, "data"))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertThatThrownBy(() -> P2PKPublicKeys.requireValid((String) null, "data"))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("never leaks key material in the exception message")
        void doesNotLeakKeyMaterial() {
            assertThatThrownBy(() -> P2PKPublicKeys.requireValid(X_ONLY, "data"))
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageNotContaining(X_ONLY);
        }

        @Test
        @DisplayName("accepts valid compressed bytes")
        void acceptsValidBytes() {
            byte[] bytes = hex(VALID_02);
            assertThat(P2PKPublicKeys.requireValid(bytes, "data").toString()).isEqualTo(VALID_02);
        }

        @Test
        @DisplayName("rejects 32-byte input")
        void rejectsShortBytes() {
            assertThatThrownBy(() -> P2PKPublicKeys.requireValid(hex(X_ONLY), "data"))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("rejects 65-byte SEC1 input that PublicKey.fromBytes would accept")
        void rejectsSec1Bytes() {
            byte[] sec1 = hex("04" + X_ONLY + X_ONLY);
            assertThatThrownBy(() -> P2PKPublicKeys.requireValid(sec1, "data"))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }
    }

    @Nested
    @DisplayName("toComparisonForm")
    class ToComparisonForm {

        @Test
        @DisplayName("treats 02 and 03 variants of one x-coordinate as the same key")
        void parityIgnored() {
            // Load-bearing: BIP-340 signs on the x-coordinate alone, so 02||x and 03||x are one
            // signing key. NUT-11 declares them duplicates of each other.
            assertThat(P2PKPublicKeys.toComparisonForm(VALID_02))
                    .isEqualTo(P2PKPublicKeys.toComparisonForm(VALID_03));
        }

        @Test
        @DisplayName("is case-insensitive")
        void caseIgnored() {
            assertThat(P2PKPublicKeys.toComparisonForm(VALID_02.toUpperCase()))
                    .isEqualTo(P2PKPublicKeys.toComparisonForm(VALID_02));
        }

        @Test
        @DisplayName("yields the lowercase x-coordinate")
        void yieldsXCoordinate() {
            assertThat(P2PKPublicKeys.toComparisonForm(VALID_03)).isEqualTo(X_ONLY);
        }

        @Test
        @DisplayName("distinguishes different x-coordinates")
        void differentKeysDiffer() {
            String other =
                    "02c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5";
            assertThat(P2PKPublicKeys.toComparisonForm(VALID_02))
                    .isNotEqualTo(P2PKPublicKeys.toComparisonForm(other));
        }
    }

    private static byte[] hex(String s) {
        return org.bouncycastle.util.encoders.Hex.decode(s);
    }
}
