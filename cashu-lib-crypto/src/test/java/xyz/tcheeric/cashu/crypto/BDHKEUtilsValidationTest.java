package xyz.tcheeric.cashu.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validation tests for BDHKE hash-to-curve input sanitisation.
 */
class BDHKEUtilsValidationTest {

    /**
     * Ensures hashing a null or empty string raises an IllegalArgumentException with context.
     */
    @Test
    void shouldRejectNullOrEmptyStringWhenHashingToCurve() {
        // Arrange
        Executable nullAction = () -> BDHKEUtils.hashToCurve((String) null);
        Executable emptyAction = () -> BDHKEUtils.hashToCurve("");

        // Act & Assert
        IllegalArgumentException nullException = assertThrows(IllegalArgumentException.class, nullAction);
        assertEquals("secret must not be null or empty", nullException.getMessage());

        IllegalArgumentException emptyException = assertThrows(IllegalArgumentException.class, emptyAction);
        assertEquals("secret must not be null or empty", emptyException.getMessage());
    }

    /**
     * Ensures hashing null or empty byte arrays raises an IllegalArgumentException with context.
     */
    @Test
    void shouldRejectNullOrEmptyBytesWhenHashingToCurve() {
        // Arrange
        Executable nullAction = () -> BDHKEUtils.hashToCurve((byte[]) null);
        Executable emptyAction = () -> BDHKEUtils.hashToCurve(new byte[0]);

        // Act & Assert
        IllegalArgumentException nullException = assertThrows(IllegalArgumentException.class, nullAction);
        assertEquals("secret must not be null or empty", nullException.getMessage());

        IllegalArgumentException emptyException = assertThrows(IllegalArgumentException.class, emptyAction);
        assertEquals("secret must not be null or empty", emptyException.getMessage());
    }
}
