package xyz.tcheeric.cashu.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.security.Provider;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Schnorr signing and verification utilities.
 */
class SchnorrTest {

    /**
     * Ensures generated signatures verify against the derived public key.
     */
    @Test
    void shouldSignAndVerifyRandomMessage() throws Exception {
        // Arrange
        byte[] privateKey = Schnorr.generatePrivateKey();
        byte[] publicKey = Schnorr.genPubKey(privateKey);
        byte[] message = generateMessage();

        // Act
        byte[] signature = Schnorr.sign(message, privateKey);
        boolean verified = Schnorr.verify(message, publicKey, signature);

        // Assert
        assertTrue(verified);
    }

    /**
     * Ensures known vectors from BIP-340 verify successfully.
     */
    @Test
    void shouldVerifyKnownVector() throws Exception {
        // Arrange
        byte[] message = Utils.sha256(Hex.decode("fc1f245f363dac26acaeb57cc457264cf6c9ccf512fdf2684a4645a3b0f59d44"));
        byte[] publicKey = Hex.decode("fc1f245f363dac26acaeb57cc457264cf6c9ccf512fdf2684a4645a3b0f59d44");
        byte[] signature = Hex.decode("991562096c11c1d8798a7557a278b81e6fed29afa020c73f3e090489687afc06d22566fe0d43795d2c39aca75fc0717a28e7c3fee2af212287f0804dbe5e9f22");

        // Act
        boolean verified = Schnorr.verify(message, publicKey, signature);

        // Assert
        assertTrue(verified);
    }

    /**
     * Ensures signature verification fails when the message is tampered with.
     */
    @Test
    void shouldFailVerificationWhenMessageChanges() throws Exception {
        // Arrange
        byte[] privateKey = Schnorr.generatePrivateKey();
        byte[] publicKey = Schnorr.genPubKey(privateKey);
        byte[] message = generateMessage();
        byte[] signature = Schnorr.sign(message, privateKey);
        byte[] tamperedMessage = message.clone();
        tamperedMessage[0] ^= 0x01;

        // Act
        boolean verified = Schnorr.verify(tamperedMessage, publicKey, signature);

        // Assert
        assertFalse(verified);
    }

    /**
     * Ensures the Bouncy Castle provider is registered only once.
     */
    @Test
    void shouldRegisterProviderOnlyOnce() {
        // Arrange
        Schnorr.generatePrivateKey();
        int countAfterFirst = countBcProviders();

        // Act
        Schnorr.generatePrivateKey();
        int countAfterSecond = countBcProviders();

        // Assert
        assertEquals(countAfterFirst, countAfterSecond);
        assertEquals(1, countAfterSecond);
    }

    private int countBcProviders() {
        int count = 0;
        for (Provider provider : Security.getProviders()) {
            if (BouncyCastleProvider.PROVIDER_NAME.equals(provider.getName())) {
                count++;
            }
        }
        return count;
    }

    private byte[] generateMessage() throws Exception {
        byte[] privateKey = Schnorr.generatePrivateKey();
        return Schnorr.genPubKey(privateKey);
    }
}
