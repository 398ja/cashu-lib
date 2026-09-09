package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A PublicKey instance should always be a point on secp256k1.
 *
 * <p>Construction checked the length and the prefix but not membership of the curve (audit L-4).
 * Most 32-byte x values have no corresponding point, so a 33-byte string starting 02 is not
 * necessarily a key. Accepting one produced an object that looked valid everywhere it was passed
 * and misbehaved only deep inside a signature or BDHKE operation, far from the input that caused
 * it.
 */
@DisplayName("PublicKey rejects points that are not on the curve")
class PublicKeyOnCurveTest {

    /** The secp256k1 generator: a real point. */
    private static final String GENERATOR =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    @Test
    @DisplayName("a real point is accepted")
    void realPointIsAccepted() {
        assertThatCode(() -> PublicKey.fromString(GENERATOR)).doesNotThrowAnyException();
        assertThat(PublicKey.fromString(GENERATOR).toString()).isEqualToIgnoringCase(GENERATOR);
    }

    @Test
    @DisplayName("a well-formed but off-curve x value is rejected")
    void offCurvePointIsRejected() {
        // Correct length, valid prefix, but x = 5 has no point on secp256k1: y^2 = 132 is not a
        // quadratic residue mod p. (x = 1 looks like the obvious choice and is in fact ON the
        // curve, which is precisely why this needs checking rather than assuming.)
        String offCurve = "02" + "0".repeat(63) + "5";

        // BouncyCastle reports this as "Invalid point compression" rather than naming the
        // curve, so the assertion is on the type: what matters is that construction fails.
        assertThatThrownBy(() -> PublicKey.fromString(offCurve))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("an all-zero key is rejected")
    void allZeroKeyIsRejected() {
        assertThatThrownBy(() -> PublicKey.fromString("02" + "00".repeat(32)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("x equal to the field prime is rejected")
    void fieldPrimeIsRejected() {
        // p itself is not a valid field element.
        String p = "02fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f";

        assertThatThrownBy(() -> PublicKey.fromString(p))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
