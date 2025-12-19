package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import xyz.tcheeric.cashu.crypto.util.Point;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicKeyTest {

    @Test
    /**
     * Verifies uncompressed x||y bytes recompress to the expected 02/03||x format.
     */
    void shouldCompressUncompressedBytesToCanonicalForm() {
        // Arrange
        String compressed = "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2";
        byte[] xBytes = Utils.hexStringToBytes(compressed.substring(2));
        Point liftedPoint = Point.liftX(xBytes);
        assertNotNull(liftedPoint);

        boolean requiresEvenY = compressed.startsWith("02");
        BigInteger yCoordinate = liftedPoint.getY();
        if (requiresEvenY != Point.hasEvenY(liftedPoint)) {
            yCoordinate = Point.getp().subtract(yCoordinate);
        }

        byte[] xCoordinate = leftPad32(liftedPoint.getX().toByteArray());
        byte[] yCoordinateBytes = leftPad32(yCoordinate.toByteArray());
        byte[] uncompressed = new byte[64];
        System.arraycopy(xCoordinate, 0, uncompressed, 0, 32);
        System.arraycopy(yCoordinateBytes, 0, uncompressed, 32, 32);

        // Act
        PublicKey fromUncompressed = PublicKey.fromBytes(uncompressed, false);
        PublicKey fromCompressed = PublicKey.fromString(compressed);

        // Assert
        assertEquals(compressed, fromUncompressed.toString());
        assertEquals(compressed, fromCompressed.toString());
    }

    @Test
    /**
     * Ensures the instance and static Schnorr key accessors return identical x-coordinates.
     */
    void shouldExposeSameSchnorrBytesFromInstanceAndStaticMethod() {
        // Arrange
        String compressed = "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2";
        PublicKey publicKey = PublicKey.fromString(compressed);

        // Act
        byte[] instanceSchnorr = publicKey.getSchnorr();
        byte[] staticSchnorr = PublicKey.getSchnorr(publicKey);

        // Assert
        assertArrayEquals(instanceSchnorr, staticSchnorr);
        assertArrayEquals(Utils.hexStringToBytes(compressed.substring(2)), instanceSchnorr);
    }

    @Test
    /**
     * Ensures compressed keys with invalid lengths throw IllegalArgumentException.
     */
    void shouldRejectInvalidCompressedLengths() {
        // Arrange
        String tooShort = "02" + "a".repeat(63);
        String tooLong = "02" + "a".repeat(65);
        Executable buildTooShort = () -> PublicKey.fromString(tooShort);
        Executable buildTooLong = () -> PublicKey.fromString(tooLong);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, buildTooShort);
        assertThrows(IllegalArgumentException.class, buildTooLong);
    }

    @Test
    /**
     * Ensures uncompressed inputs with invalid byte lengths throw IllegalArgumentException.
     */
    void shouldRejectInvalidUncompressedLengths() {
        // Arrange
        byte[] tooShort = new byte[63];
        byte[] tooLong = new byte[65];
        Executable buildTooShort = () -> PublicKey.fromBytes(tooShort);
        Executable buildTooLong = () -> PublicKey.fromBytes(tooLong);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, buildTooShort);
        assertThrows(IllegalArgumentException.class, buildTooLong);
    }

    private static byte[] leftPad32(byte[] source) {
        if (source.length == 32) {
            return source;
        }
        byte[] output = new byte[32];
        if (source.length > 32) {
            System.arraycopy(source, source.length - 32, output, 0, 32);
        } else {
            System.arraycopy(source, 0, output, 32 - source.length, source.length);
        }
        return output;
    }
}
