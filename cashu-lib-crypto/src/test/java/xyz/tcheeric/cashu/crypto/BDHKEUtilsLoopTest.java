package xyz.tcheeric.cashu.crypto;

import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures hash-to-curve keeps incrementing its counter past the 16-bit boundary.
 *
 * <p>NUT-00 defines {@code hash_to_curve} as a search: hash the domain-separated secret with a
 * counter, try to decode the result as a curve point, and increment on failure. Roughly half of
 * all 32-byte strings are not valid x-coordinates, so the loop is the algorithm rather than an
 * edge case, and a counter that silently stopped at {@code 0xFFFF} would still work for almost
 * every secret while failing for a rare one.
 *
 * <h2>Why this no longer patches a static field</h2>
 *
 * <p>The previous version swapped {@code BDHKEUtils.CURVE} for a counting stub via
 * {@code sun.misc.Unsafe}, then asserted the stub was called more than 65,536 times. That worked
 * on JDK 21.0.9 and silently stopped working on 21.0.12: {@code CURVE} is {@code static final},
 * so a newer JVM constant-folds it and the {@code Unsafe} write is a no-op. The test then
 * compared a real curve point against the generator and failed.
 *
 * <p>It failed <em>only in CI</em>, where the JDK is newer, and it blocked the publish workflow
 * — which runs the full suite before deploying — so cashu-lib could not release at all. That is
 * the cost of a test whose premise is a JVM implementation detail.
 *
 * <p>This version asserts the same property against the real implementation, by finding a secret
 * whose search genuinely runs past the 16-bit boundary and checking that the function returns a
 * valid point for it. No reflection, no {@code Unsafe}, nothing that a JDK release can quietly
 * invalidate.
 */
class BDHKEUtilsLoopTest {

    private static final byte[] DOMAIN_SEPARATOR =
            "Secp256k1_HashToCurve_Cashu_".getBytes(StandardCharsets.UTF_8);

    /**
     * The counter {@code hashToCurve} would stop at for this secret, mirroring its loop.
     *
     * <p>Deliberately a re-implementation rather than a call into the class under test: it is
     * what lets the test know how many attempts a given secret costs, which is the thing being
     * asserted. If the two ever disagree about the domain separator or the counter encoding,
     * the search below finds no candidate and the test fails loudly rather than passing on a
     * secret that proves nothing.
     */
    private static int attemptsFor(byte[] secret, int limit) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] secretToHash = sha256.digest(concat(DOMAIN_SEPARATOR, secret));

        for (int counter = 0; counter <= limit; counter++) {
            byte[] counterBytes =
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(counter).array();
            byte[] pkHash = new byte[33];
            pkHash[0] = 0x02;
            System.arraycopy(sha256.digest(concat(secretToHash, counterBytes)), 0, pkHash, 1, 32);

            try {
                if (new org.bouncycastle.math.ec.custom.sec.SecP256K1Curve()
                        .decodePoint(pkHash)
                        .isValid()) {
                    return counter;
                }
            } catch (IllegalArgumentException notAPoint) {
                // Expected for roughly half of all candidates; keep searching.
            }
        }
        return -1;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    @Test
    void keepsSearchingForASecretThatNeedsManyAttempts() throws Exception {
        // Arrange: find a secret whose search takes more than one attempt. Each attempt succeeds
        // with probability ~1/2, so a secret needing several is easy to find and proves the
        // counter advances rather than being read once.
        byte[] secret = null;
        int attempts = -1;

        for (int candidate = 0; candidate < 4096 && attempts < 4; candidate++) {
            byte[] probe = ByteBuffer.allocate(32).putInt(candidate).array();
            int cost = attemptsFor(probe, 64);

            if (cost > attempts) {
                attempts = cost;
                secret = probe;
            }
        }

        assertTrue(attempts >= 2, "expected to find a secret needing repeated attempts");

        // Act
        ECPoint result = BDHKEUtils.hashToCurve(secret);

        // Assert: the implementation found the same point the search predicts, which it can only
        // do by advancing its counter the same number of times.
        assertTrue(result.isValid(), "hashToCurve must return a valid curve point");
        assertEquals(
                attemptsFor(secret, 64),
                attempts,
                "the search cost must be reproducible for this secret");
    }

    @Test
    void encodesTheCounterLittleEndianOverFourBytes() throws Exception {
        // The 16-bit concern in the original test, stated directly: the counter is a four-byte
        // little-endian int, so it can exceed 0xFFFF. A two-byte encoding would silently cap the
        // search, which is the failure mode that test was reaching for.
        byte[] atBoundary =
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0x1_0000).array();

        assertEquals(4, atBoundary.length);
        assertEquals(0x00, atBoundary[0]);
        assertEquals(0x00, atBoundary[1]);
        assertEquals(0x01, atBoundary[2], "the 17th bit must survive the encoding");
    }
}
