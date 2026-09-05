package xyz.tcheeric.cashu.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.util.Point;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link Point#mul} was swapped from a hand-rolled double-and-add to BouncyCastle's windowed
 * multiply, to remove a timing side channel on the private key and nonce (audit M-6). The change
 * was described as behaviour-preserving. It was not, and none of the 1013 tests in the suite
 * noticed, because every BIP-340 and NUT-00 vector uses an in-range scalar and so passes
 * vacuously with respect to the difference.
 *
 * <p>These tests pin the contract at the boundaries the vectors never touch: zero, the group
 * order, multiples of it, negatives, and scalars wider than 256 bits. The old loop answered three
 * of those wrongly, and {@code Point.mul} is public on a published class, so the answers are worth
 * fixing in place rather than leaving to be rediscovered.
 */
@DisplayName("Point.mul scalar contract")
class PointMulContractTest {

    /** secp256k1 field prime, for checking that -k*G reflects k*G. */
    private static final BigInteger FIELD_PRIME = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);

    private static final BigInteger ORDER = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

    @Test
    @DisplayName("a scalar of zero gives the point at infinity")
    void zeroScalarIsInfinity() {
        assertNull(Point.mul(Point.G, BigInteger.ZERO));
    }

    @Test
    @DisplayName("a scalar equal to the group order gives the point at infinity")
    void orderScalarIsInfinity() {
        // The old loop returned a non-null infinity Point here. Any caller distinguishing "no
        // result" from "the point at infinity" sees a different object now.
        assertNull(Point.mul(Point.G, ORDER),
                "n*G is the identity, and this method reports the identity as null");
    }

    @Test
    @DisplayName("a multiple of the group order gives the point at infinity")
    void multipleOfOrderIsInfinity() {
        // The old loop returned a finite point for 2n, which was simply wrong: it iterated a
        // fixed 256 bits and never reduced.
        assertNull(Point.mul(Point.G, ORDER.multiply(BigInteger.TWO)));
        assertNull(Point.mul(Point.G, ORDER.multiply(BigInteger.valueOf(5))));
    }

    @Test
    @DisplayName("a scalar wider than 256 bits is reduced rather than truncated")
    void wideScalarIsReduced() {
        BigInteger wide = BigInteger.TWO.pow(300);

        Point actual = Point.mul(Point.G, wide);

        // The old loop ran off the end of its 256-bit iteration and returned null here.
        assertNotNull(actual, "a wide scalar must be reduced mod n, not truncated to nothing");
        assertEquals(Point.mul(Point.G, wide.mod(ORDER)), actual,
                "the answer must equal the one for the reduced scalar");
    }

    @Test
    @DisplayName("a scalar just above the order wraps to the same point as the remainder")
    void scalarJustAboveOrderWraps() {
        assertEquals(Point.mul(Point.G, BigInteger.ONE),
                Point.mul(Point.G, ORDER.add(BigInteger.ONE)));
        assertEquals(Point.mul(Point.G, BigInteger.TWO),
                Point.mul(Point.G, ORDER.add(BigInteger.TWO)));
    }

    @Test
    @DisplayName("a negative scalar normalises modulo the order")
    void negativeScalarNormalises() {
        BigInteger negative = BigInteger.valueOf(-5);

        Point actual = Point.mul(Point.G, negative);

        assertNotNull(actual);
        assertEquals(Point.mul(Point.G, negative.mod(ORDER)), actual,
                "the old signed bit iteration gave a different point for a negative scalar");
    }

    @Test
    @DisplayName("negating a scalar negates the y coordinate")
    void negationIsConsistentWithTheCurve() {
        // -k*G must be the reflection of k*G. This is what makes the negative-scalar answer the
        // correct one rather than merely a different one.
        Point positive = Point.mul(Point.G, BigInteger.valueOf(7));
        Point negative = Point.mul(Point.G, BigInteger.valueOf(-7));

        assertNotNull(positive);
        assertNotNull(negative);
        assertEquals(positive.getX(), negative.getX());
        assertEquals(FIELD_PRIME.subtract(positive.getY()), negative.getY());
    }

    @Test
    @DisplayName("a null point or scalar gives null")
    void nullInputsGiveNull() {
        assertNull(Point.mul(null, BigInteger.ONE));
        assertNull(Point.mul(Point.G, null));
    }

    @Test
    @DisplayName("ordinary in-range scalars are unaffected")
    void inRangeScalarsAreUnchanged() {
        // The property the vectors already cover, restated so a future change to the reduction
        // cannot quietly move the common case.
        Point g = Point.mul(Point.G, BigInteger.ONE);
        assertEquals(Point.G.getX(), g.getX());
        assertEquals(Point.G.getY(), g.getY());

        assertEquals(Point.add(Point.G, Point.G), Point.mul(Point.G, BigInteger.TWO));
    }
}
