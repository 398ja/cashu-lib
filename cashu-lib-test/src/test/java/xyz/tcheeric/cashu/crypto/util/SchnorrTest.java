package xyz.tcheeric.cashu.crypto.util;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.Schnorr;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SchnorrTest {

    @Test
    public void verify() throws Exception {

        byte[] privateKey = Schnorr.generatePrivateKey();
        byte[] publicKey = Schnorr.genPubKey(privateKey);
        byte[] message = generateMessage();
        byte[] signature = Schnorr.sign(message, privateKey);

        assertTrue(Schnorr.verify(message, publicKey, signature));
    }

    @Test
    public void verifyValues() throws Exception {
        String privateKey = "0a498f8f0e00c235f5959c878abb9cf5533323971d729836423076a92aa2e03a";
        String publicKey = "fc1f245f363dac26acaeb57cc457264cf6c9ccf512fdf2684a4645a3b0f59d44";
        String message = Hex.toHexString(Utils.sha256( Hex.decode("fc1f245f363dac26acaeb57cc457264cf6c9ccf512fdf2684a4645a3b0f59d44")));
        String signature = "991562096c11c1d8798a7557a278b81e6fed29afa020c73f3e090489687afc06d22566fe0d43795d2c39aca75fc0717a28e7c3fee2af212287f0804dbe5e9f22";

        assertTrue(Schnorr.verify(Hex.decode(message), Hex.decode(publicKey), Hex.decode(signature)));
    }

    @Test
    public void verifyFailsForModifiedMessage() throws Exception {
        byte[] privateKey = Schnorr.generatePrivateKey();
        byte[] publicKey = Schnorr.genPubKey(privateKey);
        byte[] message = generateMessage();
        byte[] signature = Schnorr.sign(message, privateKey);

        // change a single byte in the original message
        byte[] tamperedMessage = message.clone();
        tamperedMessage[0] ^= 0x01;

        assertFalse(Schnorr.verify(tamperedMessage, publicKey, signature));
    }

    private byte[] generateMessage() throws Exception {
        byte[] privateKey = Schnorr.generatePrivateKey();
        return Schnorr.genPubKey(privateKey);
    }
}
