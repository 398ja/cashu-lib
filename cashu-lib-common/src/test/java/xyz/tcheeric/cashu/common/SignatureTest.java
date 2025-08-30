package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.PrivateKey;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.Signature;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SignatureTest {

    // Ensures that signing a message with a private key produces a signature that verifies with the corresponding public key.
    @Test
    public void signAndVerify() throws Exception {
        PrivateKey privateKey = PrivateKey.generateRandom();
        PublicKey publicKey = PrivateKey.derivePublicKey(privateKey);
        String message = "12345678901234567890123456789012"; // 32-byte message

        Signature signature = Signature.sign(message, privateKey);

        assertTrue(Signature.verify(message, publicKey, signature));
        assertTrue(signature.verify(message, publicKey));
    }

    // Ensures verification fails if the signed message is modified.
    @Test
    public void verifyFailsForTamperedMessage() throws Exception {
        PrivateKey privateKey = PrivateKey.generateRandom();
        PublicKey publicKey = PrivateKey.derivePublicKey(privateKey);
        String message = "abcdefghijklmnopqrstuvwxyzABCDEF"; // 32-byte message
        Signature signature = Signature.sign(message, privateKey);

        String tamperedMessage = "XbcdefghijklmnopqrstuvwxyzABCDEF"; // slight modification

        assertFalse(Signature.verify(tamperedMessage, publicKey, signature));
    }
}

