package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PrivateKeyTest {

    // Verifies that a signature created with a private key verifies with its derived public key
    @Test
    public void derivePublicKeyAndVerifySignature() throws Exception {
        String message = "0123456789abcdef0123456789abcdef"; // 32 bytes
        PrivateKey priv = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");
        PublicKey pub = PrivateKey.derivePublicKey(priv);

        Signature sig = Signature.sign(message, priv);
        assertTrue(Signature.verify(message, pub, sig));
        assertTrue(sig.verify(message, pub));
    }
}

