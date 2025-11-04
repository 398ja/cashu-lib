package xyz.tcheeric.cashu.common;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.util.Point;
import xyz.tcheeric.cashu.crypto.util.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureTest {

    /**
     * Verifies equals and hashCode treat identical signatures as the same value object.
     */
    @Test
    void shouldTreatIdenticalSignaturesAsEqual() {
        // Arrange
        String signatureHex = "02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea";
        Signature firstSignature = Signature.fromString(signatureHex);
        Signature secondSignature = Signature.fromString(signatureHex);

        // Act
        boolean equalsSelf = firstSignature.equals(firstSignature);
        boolean equalsSecond = firstSignature.equals(secondSignature);
        boolean equalsFirstReverse = secondSignature.equals(firstSignature);
        int firstHash = firstSignature.hashCode();
        int secondHash = secondSignature.hashCode();

        // Assert
        assertTrue(equalsSelf);
        assertTrue(equalsSecond);
        assertTrue(equalsFirstReverse);
        assertEquals(firstHash, secondHash);
    }

    /**
     * Ensures signatures constructed from different values are not equal.
     */
    @Test
    void shouldConsiderDifferentSignaturesNotEqual() {
        // Arrange
        Signature firstSignature = Signature.fromString("02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea");
        Signature secondSignature = Signature.fromString("029e8e5050b890a7d6c0968db16bc1d5d5fa040ea1de284f6ec69d61299f671059");

        // Act
        boolean equals = firstSignature.equals(secondSignature);

        // Assert
        assertFalse(equals);
    }

    /**
     * Confirms equals returns false for objects of a different type.
     */
    @Test
    void shouldReturnFalseWhenComparingToDifferentType() {
        // Arrange
        Signature signature = Signature.fromString("02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea");
        Object differentType = "not a signature";

        // Act
        boolean equalsDifferentType = signature.equals(differentType);

        // Assert
        assertFalse(equalsDifferentType);
    }

    /**
     * Verifies that byte-based and string-based constructors produce the same signature value.
     */
    @Test
    void shouldCreateEquivalentSignaturesFromBytesAndString() {
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
        Signature fromString = Signature.fromString(compressed);
        Signature fromBytes = Signature.fromBytes(uncompressed);

        // Assert
        assertEquals(fromString, fromBytes);
        assertEquals(fromString.hashCode(), fromBytes.hashCode());
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

    /**
     * Ensures Signature.fromBytes rejects arrays that are not exactly 64 bytes long.
     */
    @Test
    void shouldRejectByteArraysWithInvalidLength() {
        // Arrange
        byte[] tooShort = new byte[63];
        byte[] tooLong = new byte[65];

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> Signature.fromBytes(tooShort));
        assertThrows(IllegalArgumentException.class, () -> Signature.fromBytes(tooLong));
    }

    /**
     * Validates a signature produced by sign verifies with both static and instance verifier methods.
     */
    @Test
    void shouldSignAndVerifyMessage() throws Exception {
        // Arrange
        String message = "0123456789abcdef0123456789abcdef";
        PrivateKey privateKey = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");
        PublicKey publicKey = PublicKey.derivePublicKey(privateKey);

        // Act
        Signature signature = Signature.sign(message, privateKey);
        boolean staticVerification = Signature.verify(message, publicKey, signature);
        boolean instanceVerification = signature.verify(message, publicKey);

        // Assert
        assertTrue(staticVerification);
        assertTrue(instanceVerification);
    }

    /**
     * Verifies signature validation fails when the message changes.
     */
    @Test
    void shouldFailVerificationWhenMessageChanges() throws Exception {
        // Arrange
        String originalMessage = "0123456789abcdef0123456789abcdef";
        String alteredMessage = "fedcba9876543210fedcba9876543210";
        PrivateKey privateKey = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");
        PublicKey publicKey = PublicKey.derivePublicKey(privateKey);
        Signature signature = Signature.sign(originalMessage, privateKey);

        // Act
        boolean staticVerification = Signature.verify(alteredMessage, publicKey, signature);
        boolean instanceVerification = signature.verify(alteredMessage, publicKey);

        // Assert
        assertFalse(staticVerification);
        assertFalse(instanceVerification);
    }
}
