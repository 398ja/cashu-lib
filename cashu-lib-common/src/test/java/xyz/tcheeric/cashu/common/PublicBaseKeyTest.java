package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicBaseKeyTest {

    private static final String PRIVATE_KEY_HEX =
            "0000000000000000000000000000000000000000000000000000000000000001";
    private static final String EXPECTED_PUBLIC_KEY_HEX =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    @Test
    /**
     * Ensures deriving a public key from a PrivateKey instance returns the generator point.
     */
    void shouldDerivePublicKeyFromPrivateKeyInstance() {
        // Arrange
        PrivateKey privateKey = PrivateKey.fromString(PRIVATE_KEY_HEX);

        // Act
        PublicKey derivedPublicKey = PublicKey.derivePublicKey(privateKey);

        // Assert
        assertEquals(PublicKey.fromString(EXPECTED_PUBLIC_KEY_HEX), derivedPublicKey);
    }

    @Test
    /**
     * Ensures deriving a public key directly from a private key hex string matches expectations.
     */
    void shouldDerivePublicKeyFromPrivateKeyString() {
        // Arrange
        String privateKeyHex = PRIVATE_KEY_HEX;

        // Act
        PublicKey derivedPublicKey = PublicKey.derivePublicKey(privateKeyHex);

        // Assert
        assertEquals(PublicKey.fromString(EXPECTED_PUBLIC_KEY_HEX), derivedPublicKey);
    }
}
