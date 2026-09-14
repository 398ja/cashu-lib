package xyz.tcheeric.cashu.crypto;

import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.util.Utils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NUT-11 thresholds count distinct public keys, not signatures (issue #265).
 *
 * <p>If they counted signatures, one key could satisfy an n-of-m by submitting twice — and since
 * Schnorr signatures are non-deterministic, that key could produce unlimited distinct valid
 * signatures over the same message. Either way: value locked to "2 of these 3 people", released
 * by one.
 *
 * <p>The rule was implemented correctly in `cashu-mint` and had no test at all. These use real
 * BIP-340 signatures rather than fixtures, because the property being checked is cryptographic.
 */
class SigningKeyCounterTest {

    private static final byte[] MESSAGE = "spend-this-proof".getBytes();

    /** The forged-multisig case: one key signing twice must count once. */
    @Test
    void oneKeySigningTwiceCannotSatisfyATwoOfTwo() {
        Signer signer = new Signer();

        int counted = SigningKeyCounter.countSigningKeys(
                List.of(signer.publicKeyHex(), signer.publicKeyHex()),
                List.of(signer.sign(MESSAGE), signer.sign(MESSAGE)),
                MESSAGE);

        assertThat(counted)
                .as("a duplicated key gets one vote; otherwise one signer clears a 2-of-2")
                .isEqualTo(1);
    }

    /**
     * Two genuinely distinct keys both count, so the rule is not simply "always 1" — a counter
     * that returned 1 unconditionally would pass the test above while breaking every real
     * multisig.
     */
    @Test
    void twoDistinctKeysBothCount() {
        Signer alice = new Signer();
        Signer bob = new Signer();

        int counted = SigningKeyCounter.countSigningKeys(
                List.of(alice.publicKeyHex(), bob.publicKeyHex()),
                List.of(alice.sign(MESSAGE), bob.sign(MESSAGE)),
                MESSAGE);

        assertThat(counted).isEqualTo(2);
    }

    /**
     * Non-determinism is the sharp edge: the same key signing the same message twice yields two
     * *different* valid signatures, so signature-counting would not even need a crafted secret.
     */
    @Test
    void twoDifferentSignaturesFromOneKeyStillCountOnce() {
        Signer signer = new Signer();
        String first = signer.sign(MESSAGE);
        String second = signer.sign(MESSAGE);

        assertThat(first).as("BIP-340 signing is randomised").isNotEqualTo(second);

        int counted = SigningKeyCounter.countSigningKeys(
                List.of(signer.publicKeyHex()), List.of(first, second), MESSAGE);

        assertThat(counted).isEqualTo(1);
    }

    /** A key that did not sign does not count, however many signatures are present. */
    @Test
    void aKeyThatDidNotSignDoesNotCount() {
        Signer signer = new Signer();
        Signer bystander = new Signer();

        int counted = SigningKeyCounter.countSigningKeys(
                List.of(bystander.publicKeyHex()), List.of(signer.sign(MESSAGE)), MESSAGE);

        assertThat(counted).isZero();
    }

    /**
     * NUT-11 carries 33-byte compressed keys while BIP-340 verifies against the 32-byte x-only
     * form. Both spellings must verify, and must deduplicate to one another rather than counting
     * twice.
     */
    @Test
    void compressedAndXOnlySpellingsOfOneKeyCountOnce() {
        Signer signer = new Signer();
        String compressed = "02" + signer.publicKeyHex();

        int counted = SigningKeyCounter.countSigningKeys(
                List.of(compressed, signer.publicKeyHex()),
                List.of(signer.sign(MESSAGE)),
                MESSAGE);

        assertThat(counted)
                .as("the same key written two ways is still one key")
                .isEqualTo(1);
    }

    /** A malformed entry must not veto the valid ones alongside it. */
    @Test
    void aMalformedSignatureDoesNotBlockAValidOne() {
        Signer signer = new Signer();

        int counted = SigningKeyCounter.countSigningKeys(
                List.of(signer.publicKeyHex()),
                List.of("not-hex", signer.sign(MESSAGE)),
                MESSAGE);

        assertThat(counted).isEqualTo(1);
    }

    /** Null and empty inputs count nothing rather than throwing. */
    @Test
    void missingInputsCountNothing() {
        Signer signer = new Signer();

        assertThat(SigningKeyCounter.countSigningKeys(null, List.of(), MESSAGE)).isZero();
        assertThat(SigningKeyCounter.countSigningKeys(List.of(), null, MESSAGE)).isZero();
        assertThat(SigningKeyCounter.countSigningKeys(
                List.of(signer.publicKeyHex()), List.of(), MESSAGE)).isZero();
    }

    /** A real secp256k1 keypair, so the test exercises BIP-340 rather than a stub. */
    private static final class Signer {
        private final byte[] privateKey = Schnorr.generatePrivateKey();

        String publicKeyHex() {
            return Hex.toHexString(Schnorr.genPubKey(privateKey));
        }

        String sign(byte[] message) {
            try {
                return Hex.toHexString(Schnorr.sign(Utils.sha256(message), privateKey));
            } catch (Exception e) {
                throw new IllegalStateException("signing failed in test setup", e);
            }
        }
    }
}
