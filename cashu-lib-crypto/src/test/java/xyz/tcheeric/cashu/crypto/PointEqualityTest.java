package xyz.tcheeric.cashu.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.util.Point;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Point} declared {@code equals(Point)}, which is an overload and not an override of
 * {@link Object#equals(Object)}. Any comparison through an {@code Object} reference, which is
 * every collection lookup and every assertion library, fell back to identity: two Points with
 * identical coordinates were unequal. The compiler had nothing to say, because the overload is
 * perfectly legal, and there was no {@code hashCode} at all.
 *
 * <p>Found while writing tests for the Point.mul contract, where assertEquals on two equal points
 * failed for reasons that had nothing to do with the multiply.
 */
@DisplayName("Point equality")
class PointEqualityTest {

    @Test
    @DisplayName("points with the same coordinates are equal through an Object reference")
    void equalThroughObjectReference() {
        Point a = Point.mul(Point.G, BigInteger.valueOf(7));
        Point b = Point.mul(Point.G, BigInteger.valueOf(7));

        Object asObject = b;
        assertTrue(a.equals(asObject),
                "the overload meant this compared identity and was false");
        assertEquals(a, b);
    }

    @Test
    @DisplayName("points with different coordinates are unequal")
    void differentPointsAreUnequal() {
        assertNotEquals(Point.mul(Point.G, BigInteger.valueOf(7)),
                Point.mul(Point.G, BigInteger.valueOf(8)));
    }

    @Test
    @DisplayName("a point is not equal to a non-point or to null")
    void unequalToOtherTypes() {
        Point a = Point.mul(Point.G, BigInteger.valueOf(7));

        assertNotEquals(a, "not a point");
        assertNotEquals(a, null);
    }

    @Test
    @DisplayName("equal points have equal hash codes, so collections work")
    void hashCodeAgreesWithEquals() {
        Point a = Point.mul(Point.G, BigInteger.valueOf(7));
        Point b = Point.mul(Point.G, BigInteger.valueOf(7));

        assertEquals(a.hashCode(), b.hashCode());

        Set<Point> set = new HashSet<>(List.of(a));
        assertTrue(set.contains(b), "a point must be findable in a set by value");

        Map<Point, String> map = new HashMap<>();
        map.put(a, "seven");
        assertEquals("seven", map.get(b), "a point must work as a map key");
    }

    @Test
    @DisplayName("equality is reflexive, symmetric and consistent")
    void equalityContractHolds() {
        Point a = Point.mul(Point.G, BigInteger.valueOf(7));
        Point b = Point.mul(Point.G, BigInteger.valueOf(7));

        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(b, a);
    }
}
