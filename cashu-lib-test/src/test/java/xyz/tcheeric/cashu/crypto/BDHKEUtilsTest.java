package xyz.tcheeric.cashu.crypto;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class BDHKEUtilsTest {

    @Test
    public void hashToCurveUsesUtf8() {
        String secret = "hello world";
        byte[] expected = BDHKEUtils.hashToCurve(secret.getBytes(StandardCharsets.UTF_8)).getEncoded(true);
        byte[] actual = BDHKEUtils.hashToCurve(secret);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void hashToCurveHexProducesOriginalBehaviour() {
        String hexSecret = "41"; // hex for 'A'
        byte[] expected = BDHKEUtils.hashToCurve(Utils.hexStringToBytes(hexSecret)).getEncoded(true);
        byte[] actual = BDHKEUtils.hashToCurveHex(hexSecret);
        assertArrayEquals(expected, actual);

        byte[] utf8 = BDHKEUtils.hashToCurve(hexSecret);
        assertFalse(Arrays.equals(expected, utf8));
    }
}
