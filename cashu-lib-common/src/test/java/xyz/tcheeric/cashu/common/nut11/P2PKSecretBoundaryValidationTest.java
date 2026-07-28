package xyz.tcheeric.cashu.common.nut11;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validation has to bite at the boundary. Deserialization is the enforcement point — a malformed
 * secret must never become a live object — and the constructors and key setters catch
 * construction-side mistakes at the point they are made rather than at serialization.
 */
class P2PKSecretBoundaryValidationTest {

    private static final String KEY_A =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String KEY_B =
            "02c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5";

    @Nested
    @DisplayName("constructors")
    class Constructors {

        @Test
        @DisplayName("rejects data that is not a public key")
        void rejectsBadData() {
            assertThatThrownBy(() -> new P2PKSecret(Hex.decode("deadbeef")))
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("data");
        }

        @Test
        @DisplayName("rejects bad data in the three-arg form too")
        void rejectsBadDataWithFlags() {
            assertThatThrownBy(() -> new P2PKSecret(
                    Hex.decode("deadbeef"), 1, P2PKSecret.SignatureFlag.SIG_ALL))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("accepts a valid compressed key")
        void acceptsValidKey() {
            assertThatCode(() -> new P2PKSecret(Hex.decode(KEY_A))).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("key setters")
    class KeySetters {

        private P2PKSecret secret() {
            return new P2PKSecret(Hex.decode(KEY_A));
        }

        @Test
        @DisplayName("addPubKey rejects a non-key")
        void addPubKey() {
            assertThatThrownBy(() -> secret().addPubKey("pk1"))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("setPubKeys rejects a non-key")
        void setPubKeys() {
            assertThatThrownBy(() -> secret().setPubKeys(List.of("pk1")))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("addRefund rejects a non-key")
        void addRefund() {
            assertThatThrownBy(() -> secret().addRefund("refund1"))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("setRefund rejects a non-key")
        void setRefund() {
            assertThatThrownBy(() -> secret().setRefund(List.of("refund1")))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("accepts valid keys")
        void acceptsValid() {
            assertThatCode(() -> {
                P2PKSecret s = secret();
                s.addPubKey(KEY_B);
                s.addRefund(KEY_B);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("deserialization")
    class Deserialization {

        private String nut10(String dataHex, String tags) {
            return "[\"P2PK\",{\"nonce\":\""
                    + "859d4935c4907062a6297cf4e663e2835d90d97ecdd510745d32f6816323a41f"
                    + "\",\"data\":\"" + dataHex + "\",\"tags\":[" + tags + "]}]";
        }

        @Test
        @DisplayName("a valid P2PK secret still parses")
        void validParses() {
            Secret secret = SecretUtil.toSecret(nut10(KEY_A, "[\"sigflag\",\"SIG_INPUTS\"]"));
            assertThat(secret).isInstanceOf(P2PKSecret.class);
        }

        @Test
        @DisplayName("a malformed lock key is rejected at parse time, not stored")
        void rejectsBadData() {
            assertThatThrownBy(() -> SecretUtil.toSecret(nut10("deadbeef", "")))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("a malformed key in the pubkeys tag is rejected at parse time")
        void rejectsBadPubkey() {
            assertThatThrownBy(() -> SecretUtil.toSecret(
                    nut10(KEY_A, "[\"pubkeys\",\"pk1\"]")))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("a duplicate key across the pathway is rejected at parse time")
        void rejectsDuplicate() {
            String key03 = "03" + KEY_A.substring(2);
            assertThatThrownBy(() -> SecretUtil.toSecret(
                    nut10(KEY_A, "[\"pubkeys\",\"" + key03 + "\"]")))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("the exception is an IllegalArgumentException, so existing handlers still catch it")
        void staysIllegalArgument() {
            assertThatThrownBy(() -> SecretUtil.toSecret(nut10("deadbeef", "")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("a malformed lock is rejected, not routed down the bearer-secret fall-through")
        void doesNotDegradeToBearerSecret() {
            // toSecret catches Exception around array parsing and falls through to
            // RandomStringSecret. A rejected P2PK lock taking that path loses its spending
            // condition entirely, leaving the proof spendable by anyone holding it.
            //
            // Reaching the fall-through is detectable: RandomStringSecret.fromString hex-decodes
            // its input, so the JSON array surfaces as a DecoderException instead of the
            // validation failure. Either outcome means the lock was dropped rather than refused.
            assertThatThrownBy(() -> SecretUtil.toSecret(nut10("deadbeef", "")))
                    .as("must fail validation, not fall through to an unlocked secret")
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }
    }
}
