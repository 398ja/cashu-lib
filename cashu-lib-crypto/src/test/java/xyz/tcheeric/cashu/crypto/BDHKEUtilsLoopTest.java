package xyz.tcheeric.cashu.crypto;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.junit.jupiter.api.Test;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures hash-to-curve continues searching beyond 16-bit space when needed.
 */
class BDHKEUtilsLoopTest {

    /**
     * Replaces the internal curve with one that fails for the first 70k attempts,
     * verifying the implementation keeps iterating until a valid point is found.
     */
    @Test
    void shouldSearchBeyondSixteenBitsWhenHashingToCurve() throws Exception {
        // Arrange
        Unsafe unsafe = obtainUnsafe();
        BDHKEUtils.pointToHex(ECNamedCurveTable.getParameterSpec("secp256k1").getG());

        Field curveField = BDHKEUtils.class.getDeclaredField("CURVE");
        Object base = unsafe.staticFieldBase(curveField);
        long offset = unsafe.staticFieldOffset(curveField);
        SecP256K1Curve originalCurve = (SecP256K1Curve) unsafe.getObject(base, offset);

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPoint generatorPoint = originalCurve.decodePoint(spec.getG().getEncoded(true));
        CountingCurve countingCurve = new CountingCurve(generatorPoint);
        unsafe.putObject(base, offset, countingCurve);

        try {
            byte[] secret = new byte[32];

            // Act
            ECPoint result = BDHKEUtils.hashToCurve(secret);

            // Assert
            assertEquals(generatorPoint, result);
            assertTrue(countingCurve.calls > 65_536);
        } finally {
            unsafe.putObject(base, offset, originalCurve);
        }
    }

    private Unsafe obtainUnsafe() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

    private static final class CountingCurve extends SecP256K1Curve {
        private final ECPoint point;
        int calls = 0;

        CountingCurve(ECPoint point) {
            this.point = point;
        }

        @Override
        public ECPoint decodePoint(byte[] encoded) {
            if (calls++ < 70_000) {
                throw new IllegalArgumentException("invalid point");
            }
            return point;
        }
    }
}
