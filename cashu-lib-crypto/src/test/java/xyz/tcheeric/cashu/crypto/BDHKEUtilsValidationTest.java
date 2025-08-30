package xyz.tcheeric.cashu.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BDHKEUtilsValidationTest {

    @Test
    public void hashToCurveStringNullOrEmpty() {
        IllegalArgumentException nullEx = assertThrows(IllegalArgumentException.class,
                () -> BDHKEUtils.hashToCurve((String) null));
        assertEquals("secret must not be null or empty", nullEx.getMessage());

        IllegalArgumentException emptyEx = assertThrows(IllegalArgumentException.class,
                () -> BDHKEUtils.hashToCurve(""));
        assertEquals("secret must not be null or empty", emptyEx.getMessage());
    }

    @Test
    public void hashToCurveBytesNullOrEmpty() {
        IllegalArgumentException nullEx = assertThrows(IllegalArgumentException.class,
                () -> BDHKEUtils.hashToCurve((byte[]) null));
        assertEquals("secret must not be null or empty", nullEx.getMessage());

        IllegalArgumentException emptyEx = assertThrows(IllegalArgumentException.class,
                () -> BDHKEUtils.hashToCurve(new byte[0]));
        assertEquals("secret must not be null or empty", emptyEx.getMessage());
    }
}

