package xyz.tcheeric.cashu.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The legacy-encoding sunset needs a trigger, not just a switch (issue #264).
 *
 * <p>{@code SecretEncoding} documents that the legacy path "should not be the price forever" and
 * names the property that turns it off. What it could not tell an operator is <em>when</em>
 * turning it off is safe: flipping it while pre-migration proofs are still unspent invalidates
 * real money, and nothing measured whether any remained. A documented plan with no trigger is a
 * plan nobody can act on.
 *
 * <p>These tests pin the counter that supplies the trigger: it must count a proof that verifies
 * only under the legacy encoding, and must not count one that verifies under the spec encoding —
 * otherwise the number would never reach zero and the sunset would stay just as untakeable.
 */
class LegacyEncodingUsageTest {

    private static final ECNamedCurveParameterSpec CURVE =
            ECNamedCurveTable.getParameterSpec("secp256k1");

    @BeforeEach
    void resetCounter() {
        LegacyEncodingUsage.reset();
    }

    /** A secret that verifies under the spec encoding must not count as legacy usage. */
    @Test
    void aSpecEncodedProofIsNotCounted() {
        String secret = "a-plain-text-secret";
        BigInteger key = BigInteger.valueOf(12345);
        ECPoint commitment = commitmentFor(secret, SecretEncoding.SPEC, key);

        assertThat(BDHKEUtils.verify(secret, key, commitment)).isTrue();
        assertThat(LegacyEncodingUsage.legacyOnlyVerifications())
                .as("a proof that verifies under the spec encoding does not depend on the "
                        + "legacy path, so counting it would keep the number permanently "
                        + "non-zero and the sunset permanently untakeable")
                .isZero();
    }

    /**
     * A hex secret whose commitment was made under the legacy encoding verifies only via the
     * fallback, and must be counted — this is the proof that would break if the path were
     * removed today.
     */
    @Test
    void aLegacyOnlyProofIsCounted() {
        // 32 bytes of hex: the legacy encoding decodes this to raw bytes, the spec encoding
        // hashes the text, so the two commit to different curve points.
        String hexSecret = "407915bc212be61a77e3e6d2aeb4c727980bda51cd06a6afc29e2861768a7837";
        BigInteger key = BigInteger.valueOf(67890);
        ECPoint legacyCommitment = commitmentFor(hexSecret, SecretEncoding.LEGACY_HEX, key);

        assertThat(BDHKEUtils.verify(hexSecret, key, legacyCommitment))
                .as("the legacy fallback still accepts a pre-migration proof")
                .isTrue();
        assertThat(LegacyEncodingUsage.legacyOnlyVerifications())
                .as("a proof that only the legacy path accepts is exactly what the operator "
                        + "needs to know about")
                .isEqualTo(1);
    }

    /** A failed verification counts as nothing, under either encoding. */
    @Test
    void aFailedVerificationIsNotCounted() {
        String secret = "some-secret";
        ECPoint wrongCommitment = commitmentFor("a-different-secret", SecretEncoding.SPEC,
                BigInteger.valueOf(11111));

        assertThat(BDHKEUtils.verify(secret, BigInteger.valueOf(11111), wrongCommitment)).isFalse();
        assertThat(LegacyEncodingUsage.legacyOnlyVerifications()).isZero();
    }

    /** The counter accumulates, so a gauge reading it sees the whole window. */
    @Test
    void theCounterAccumulates() {
        String hexSecret = "407915bc212be61a77e3e6d2aeb4c727980bda51cd06a6afc29e2861768a7837";
        BigInteger key = BigInteger.valueOf(67890);
        ECPoint legacyCommitment = commitmentFor(hexSecret, SecretEncoding.LEGACY_HEX, key);

        BDHKEUtils.verify(hexSecret, key, legacyCommitment);
        BDHKEUtils.verify(hexSecret, key, legacyCommitment);

        assertThat(LegacyEncodingUsage.legacyOnlyVerifications()).isEqualTo(2);
    }

    /** {@code C = k * hash_to_curve(encoding(secret))}, i.e. what a mint would have signed. */
    private static ECPoint commitmentFor(String secret, SecretEncoding encoding, BigInteger key) {
        ECPoint y = BDHKEUtils.hashToCurve(encoding.encode(secret));
        return y.multiply(key).normalize();
    }
}
