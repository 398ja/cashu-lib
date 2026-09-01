package xyz.tcheeric.cashu.common.nut11;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NUT-11 names the conditions that make a P2PK secret malformed, each closing "the Proof MUST be
 * rejected as unspendable".
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/11.md">NUT-11</a>
 */
class P2PKSecretValidateTest {

    /** secp256k1 generator, even-y. */
    private static final String KEY_A_02 =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    /** Same x-coordinate as {@link #KEY_A_02}, odd-y — the same signing key under BIP-340. */
    private static final String KEY_A_03 =
            "0379be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    /** A different point (2G). */
    private static final String KEY_B =
            "02c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5";

    /** A third point (3G). */
    private static final String KEY_C =
            "02f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9";

    private static P2PKSecret lockedTo(String pubkey) {
        return new P2PKSecret(Hex.decode(pubkey));
    }

    @Nested
    @DisplayName("well-formed secrets")
    class WellFormed {

        @Test
        @DisplayName("a plain single-key lock passes")
        void singleKey() {
            assertThatCode(() -> lockedTo(KEY_A_02).validate()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a multisig lock with a matching threshold passes")
        void multisig() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.addPubKey(KEY_B);
            secret.setNSigs(2); // data + one extra key = 2 keys in the main pathway
            assertThatCode(secret::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("the same key may appear in both the main and the refund pathway")
        void crossPathwayReuseAllowed() {
            // NUT-11 forbids duplicates *within* a pathway, and explicitly permits the same key
            // across the two.
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.addRefund(KEY_A_02);
            assertThatCode(secret::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("an absent n_sigs tag is not malformed — it defaults to 1")
        void absentNSigsDefaults() {
            assertThatCode(() -> lockedTo(KEY_B).validate()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("n_sigs_refund is not checked when there is no refund pathway")
        void refundThresholdIgnoredWithoutRefundKeys() {
            // getNSigsRefund() defaults to 1, but with zero refund keys that default must not be
            // read as "1 exceeds 0 keys" and condemn every ordinary secret.
            P2PKSecret secret = lockedTo(KEY_A_02);
            assertThatCode(secret::validate).doesNotThrowAnyException();
        }
    }

    /**
     * The constructors and key setters now reject bad keys up front, so these inject through the
     * raw tag API instead — which is the shape a secret has when it arrives off the wire, before
     * anything has validated it. That is exactly the state validate() exists to catch.
     */
    @Nested
    @DisplayName("invalid public keys")
    class InvalidKeys {

        @Test
        @DisplayName("rejects a data field that is not a public key")
        void rejectsBadData() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.setData(Hex.decode("deadbeef"));
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("data");
        }

        @Test
        @DisplayName("rejects a bad key in the pubkeys tag, naming its position")
        void rejectsBadPubkey() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.setTag(P2PKSecret.P2PKTag.pubkeys.name(), List.of(KEY_B, "pk1"));
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("pubkeys[1]");
        }

        @Test
        @DisplayName("rejects a bad key in the refund tag")
        void rejectsBadRefund() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.setTag(P2PKSecret.P2PKTag.refund.name(), List.of("refund1"));
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("refund[0]");
        }
    }

    @Nested
    @DisplayName("duplicate keys within a pathway")
    class DuplicateKeys {

        @Test
        @DisplayName("rejects 02||x in data against 03||x in pubkeys")
        void rejectsParityVariantDuplicate() {
            // The spec's own example. Comparing full compressed hex would miss this.
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.addPubKey(KEY_A_03);
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("duplicate");
        }

        @Test
        @DisplayName("rejects the same key twice in pubkeys, ignoring case")
        void rejectsCaseVariantDuplicate() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.addPubKey(KEY_B);
            secret.addPubKey(KEY_B.toUpperCase());
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("duplicate");
        }

        @Test
        @DisplayName("rejects a duplicate within the refund pathway")
        void rejectsRefundDuplicate() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.addRefund(KEY_B);
            secret.addRefund(KEY_B);
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("duplicate");
        }
    }

    @Nested
    @DisplayName("duplicate tags")
    class DuplicateTags {

        @Test
        @DisplayName("rejects a tag that appears more than once")
        void rejectsRepeatedTag() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.setSigFlag(P2PKSecret.SignatureFlag.SIG_INPUTS);
            // addTag appends without replacing, so this yields two sigflag tags.
            secret.addTag(P2PKSecret.P2PKTag.sigflag.name(),
                    List.of(P2PKSecret.SignatureFlag.SIG_ALL));
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("sigflag");
        }
    }

    @Nested
    @DisplayName("signature thresholds")
    class Thresholds {

        @Test
        @DisplayName("rejects n_sigs of zero")
        void rejectsZero() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.setNSigs(0);
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("n_sigs");
        }

        @Test
        @DisplayName("rejects a negative n_sigs")
        void rejectsNegative() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.setNSigs(-2);
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("n_sigs");
        }

        @Test
        @DisplayName("rejects n_sigs exceeding the main pathway key count")
        void rejectsExcessive() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.addPubKey(KEY_B);   // 2 keys total
            secret.setNSigs(3);
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("n_sigs");
        }

        @Test
        @DisplayName("rejects n_sigs_refund exceeding the refund pathway key count")
        void rejectsExcessiveRefund() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.addRefund(KEY_B);   // 1 refund key
            secret.setNSigsRefund(2);
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("n_sigs_refund");
        }

        @Test
        @DisplayName("accepts n_sigs equal to the key count")
        void acceptsExactThreshold() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.addPubKey(KEY_B);
            secret.addPubKey(KEY_C);
            secret.setNSigs(3);
            assertThatCode(secret::validate).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("signature flag")
    class SigFlag {

        @Test
        @DisplayName("rejects an unrecognised sigflag value")
        void rejectsUnknownFlag() {
            P2PKSecret secret = lockedTo(KEY_A_02);
            secret.setTag(P2PKSecret.P2PKTag.sigflag.name(), List.of("SIG_SOMETHING_ELSE"));
            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("sigflag");
        }

        @Test
        @DisplayName("accepts both specified sigflag values")
        void acceptsKnownFlags() {
            for (P2PKSecret.SignatureFlag flag : P2PKSecret.SignatureFlag.values()) {
                P2PKSecret secret = lockedTo(KEY_A_02);
                secret.setSigFlag(flag);
                assertThatCode(secret::validate).doesNotThrowAnyException();
            }
        }
    }
}
