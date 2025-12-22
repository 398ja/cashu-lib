package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateKeyTest {

    @Test
    /**
     * Ensures signatures created with a private key verify against the derived public key.
     */
    void shouldVerifySignaturesWithDerivedPublicKey() throws Exception {
        // Arrange
        String message = "0123456789abcdef0123456789abcdef";
        PrivateKey privateKey = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");
        PublicKey publicKey = PrivateKey.derivePublicKey(privateKey);

        // Act
        Signature signature = Signature.sign(message, privateKey);
        boolean staticVerification = Signature.verify(message, publicKey, signature);
        boolean instanceVerification = signature.verify(message, publicKey);

        // Assert
        assertTrue(staticVerification);
        assertTrue(instanceVerification);
    }
}
