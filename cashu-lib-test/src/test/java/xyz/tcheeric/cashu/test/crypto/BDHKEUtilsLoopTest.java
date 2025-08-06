package xyz.tcheeric.cashu.test.crypto;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BDHKEUtilsLoopTest {

    @Test
    public void hashToCurveSearchesBeyondSixteenBits() throws Exception {
        // Obtain Unsafe instance
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);

        // Access and replace the static curve field
        Field curveField = BDHKEUtils.class.getDeclaredField("CURVE");
        Object base = unsafe.staticFieldBase(curveField);
        long offset = unsafe.staticFieldOffset(curveField);
        SecP256K1Curve originalCurve = (SecP256K1Curve) unsafe.getObject(base, offset);

        class CountingCurve extends SecP256K1Curve {
            private final ECPoint point;
            int calls = 0;
            CountingCurve(ECPoint point) {
                this.point = point;
            }
            @Override
            public ECPoint decodePoint(byte[] encoded) {
                if (calls++ < 70000) {
                    throw new IllegalArgumentException("invalid point");
                }
                return point;
            }
        }

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPoint point = originalCurve.decodePoint(spec.getG().getEncoded(true));
        CountingCurve countingCurve = new CountingCurve(point);
        unsafe.putObject(base, offset, countingCurve);

        try {
            byte[] secret = new byte[32];
            ECPoint result = BDHKEUtils.hashToCurve(secret);
            assertEquals(countingCurve.point, result);
            assertTrue(countingCurve.calls > 65536);
        } finally {
            // Restore original curve
            unsafe.putObject(base, offset, originalCurve);
        }
    }
}
