package xyz.tcheeric.cashu.crypto.util;

import lombok.NonNull;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("SuspiciousNameCombination")
public class Point {

    final static private BigInteger p = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
    final static private BigInteger n = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

    final static public Point G = new Point(
            new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16),
            new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16)
    );

    private static final BigInteger BI_TWO = BigInteger.valueOf(2);

    /** secp256k1, for scalar multiplication; see {@link #mul}. */
    private static final org.bouncycastle.math.ec.custom.sec.SecP256K1Curve CURVE =
            new org.bouncycastle.math.ec.custom.sec.SecP256K1Curve();
    private final Pair<BigInteger, BigInteger> pair;

    public Point(BigInteger x, BigInteger y) {
        pair = Pair.of(x, y);
    }

    public Point(byte[] b0, byte[] b1) {
        pair = Pair.of(new BigInteger(1, b0), new BigInteger(1, b1));
    }

    public Point(String hex) {
        byte[] x = Utils.hexStringToBytes(hex.substring(0, 64));
        byte[] y = Utils.hexStringToBytes(hex.substring(64));
        pair = Pair.of(new BigInteger(1, x), new BigInteger(1, y));
    }

    public static BigInteger getp() {
        return p;
    }

    public static BigInteger getn() {
        return n;
    }

    @SuppressWarnings("SameReturnValue")
    public static Point getG() {
        return G;
    }

    public BigInteger getX() {
        return pair.getLeft();
    }

    public BigInteger getY() {
        return pair.getRight();
    }

    public static BigInteger getX(Point P) {
        assert !P.isInfinite();
        return P.getX();
    }

    public static BigInteger getY(Point P) {
        assert !P.isInfinite();
        return P.getY();
    }

    public Pair<BigInteger, BigInteger> getPair() {
        return pair;
    }

    public boolean isInfinite() {
        return pair == null || pair.getLeft() == null || pair.getRight() == null;
    }

    public static boolean isInfinite(Point P) {
        return P.isInfinite();
    }

    public Point add(@NonNull Point P) {
        return add(this, P);
    }

    public static Point add(Point P1, Point P2) {

        if ((P1 != null && P2 != null && P1.isInfinite() && P2.isInfinite())) {
            return infinityPoint();
        }
        if (P1 == null || P1.isInfinite()) {
            return P2;
        }
        if (P2 == null || P2.isInfinite()) {
            return P1;
        }
        if (P1.getX().equals(P2.getX()) && !P1.getY().equals(P2.getY())) {
            return infinityPoint();
        }

        BigInteger lam;
        if (P1.equals(P2)) {
            BigInteger base = P2.getY().multiply(BI_TWO);
            lam = (BigInteger.valueOf(3L).multiply(P1.getX()).multiply(P1.getX()).multiply(base.modPow(p.subtract(BI_TWO), p))).mod(p);
        } else {
            BigInteger base = P2.getX().subtract(P1.getX());
            lam = ((P2.getY().subtract(P1.getY())).multiply(base.modPow(p.subtract(BI_TWO), p))).mod(p);
        }

        BigInteger x3 = (lam.multiply(lam).subtract(P1.getX()).subtract(P2.getX())).mod(p);
        return new Point(x3, lam.multiply(P1.getX().subtract(x3)).subtract(P1.getY()).mod(p));
    }

    /**
     * Scalar multiplication, delegated to BouncyCastle.
     *
     * <p>This used to be a textbook double-and-add: for each of the 256 bits, always double, and
     * add only when the bit is set. The number of point additions is therefore the Hamming
     * weight of the scalar, and the scalar here is a private key
     * ({@code Schnorr.sign} line 112) or a per-signature nonce (line 133). That is a timing side
     * channel on exactly the two values that must not leak (audit M-6), and BigInteger's own
     * operations are variable-time on top of it.
     *
     * <p>BouncyCastle's {@code ECPoint.multiply} uses a windowed comb with the countermeasures
     * that implementation has accumulated, and it is already a dependency of this module. Using
     * it is strictly better than maintaining a hand-rolled ladder here: writing constant-time
     * arithmetic over {@code BigInteger} is not achievable anyway, since BigInteger allocates and
     * branches on magnitude.
     *
     * <h2>Contract, and how it differs from the old loop</h2>
     *
     * <p>The swap was described as behaviour-preserving. It is not, and since this is
     * {@code public static} on a published class the difference is worth stating rather than
     * leaving for an external caller to discover:
     *
     * <ul>
     *   <li><b>The scalar is reduced mod n.</b> The old loop iterated a fixed 256 bits and never
     *       reduced, so a scalar of {@code 2^300} fell off the end and produced {@code null},
     *       while {@code 2n} produced a finite point. Both were wrong. Now {@code 2^300} gives
     *       the mathematically correct point and {@code 2n} gives infinity.</li>
     *   <li><b>Infinity is {@code null}, not a non-null infinity Point.</b> The old code could
     *       return {@link #infinityPoint()}, a {@code Point} with {@code x} and {@code y} both
     *       null. A caller distinguishing "no result" from "the point at infinity" sees a
     *       different object now. {@link #add} accepts both, which is why no in-repo caller
     *       broke.</li>
     *   <li><b>Negative scalars normalise.</b> The old signed bit iteration returned a different
     *       point than {@code n mod order} does. The new answer is the correct one.</li>
     * </ul>
     *
     * <p>Every in-repo caller was traced and none can reach a divergent scalar: {@code Schnorr}
     * range-checks its private key and nonce, and the one place a scalar of exactly {@code n} can
     * arise ({@code Schnorr} line 209, when {@code e == 0}) feeds {@link #add}, which null-checks.
     *
     * @param n scalar; reduced modulo the group order, so any value is accepted
     * @return the resulting point, or {@code null} for the point at infinity, which includes the
     *         cases {@code n == 0} and {@code n} a non-zero multiple of the group order
     */
    public static Point mul(Point P, BigInteger n) {
        if (P == null || n == null || n.signum() == 0) {
            return null;
        }
        org.bouncycastle.math.ec.ECPoint bcPoint = CURVE.createPoint(P.getX(), P.getY());
        org.bouncycastle.math.ec.ECPoint result = bcPoint.multiply(n.mod(Point.n)).normalize();
        if (result.isInfinity()) {
            return null;
        }
        return new Point(result.getAffineXCoord().toBigInteger(),
                result.getAffineYCoord().toBigInteger());
    }

    public boolean hasEvenY() {
        return hasEvenY(this);
    }

    public static boolean hasEvenY(Point P) {
        return P.getY().mod(BI_TWO).compareTo(BigInteger.ZERO) == 0;
    }

    public static boolean isSquare(BigInteger x) {
        return x.modPow(p.subtract(BigInteger.ONE).mod(BI_TWO), p).longValue() == 1L;
    }

    public boolean hasSquareY() {
        return hasSquareY(this);
    }

    public static boolean hasSquareY(Point P) {
        assert !isInfinite(P);
        return isSquare(P.getY());
    }

    public static byte[] taggedHash(String tag, byte[] msg) throws NoSuchAlgorithmException {

        byte[] tagHash = Utils.sha256(tag.getBytes());
        int len = (tagHash.length * 2) + msg.length;
        byte[] buf = new byte[len];
        System.arraycopy(tagHash, 0, buf, 0, tagHash.length);
        System.arraycopy(tagHash, 0, buf, tagHash.length, tagHash.length);
        System.arraycopy(msg, 0, buf, tagHash.length * 2, msg.length);

        return Utils.sha256(buf);
    }

    public byte[] toBytes() {
        return bytesFromPoint(this);
    }

    public static byte[] bytesFromPoint(Point P) {
        return Utils.bytesFromBigInteger(P.getX());
    }

    // previously 'pointFromBytes()'
    public static Point liftX(byte[] b) {

        BigInteger x = Utils.bigIntFromBytes(b);
        if (x.compareTo(p) >= 0) {
            return null;
        }
        BigInteger y_sq = x.modPow(BigInteger.valueOf(3L), p).add(BigInteger.valueOf(7L)).mod(p);
        BigInteger y = y_sq.modPow(p.add(BigInteger.ONE).divide(BigInteger.valueOf(4L)), p);

        if (y.modPow(BI_TWO, p).compareTo(y_sq) != 0) {
            return null;
        } else {
            return new Point(x, y.and(BigInteger.ONE).compareTo(BigInteger.ZERO) == 0 ? y : p.subtract(y));
        }
    }

    public static Point infinityPoint() {
        return new Point(null, (BigInteger) null);
    }

    /**
     * Value equality on the affine coordinates.
     *
     * <p>This was previously declared as {@code equals(Point)}, which is an overload rather than
     * an override: any comparison through an {@code Object} reference, which includes every
     * collection lookup and every assertion library, silently fell back to
     * {@link Object#equals(Object)} and compared identity. Two Points with the same coordinates
     * were unequal, and the compiler had nothing to say about it because the overload is legal.
     *
     * <p>Kept working for callers that pass a {@code Point} statically, by widening the parameter
     * rather than adding a second method, so there is one definition of equality instead of two
     * that can disagree.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Point point)) {
            return false;
        }
        return getPair().equals(point.getPair());
    }

    /**
     * Required with {@link #equals}: a Point in a HashSet or as a HashMap key would otherwise be
     * unfindable, which is the failure mode that hides longest.
     */
    @Override
    public int hashCode() {
        return getPair().hashCode();
    }
}
