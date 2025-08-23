package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicKeyTest {

    // Ensures deriving a public key from a PrivateKey instance matches the result from PrivateKey.derivePublicKey.
    @Test
    public void deriveFromPrivateKey() {
        PrivateKey privateKey = PrivateKey.generateRandom();
        PublicKey expected = PrivateKey.derivePublicKey(privateKey);
        PublicKey actual = PublicKey.derivePublicKey(privateKey);
        assertEquals(expected, actual);
    }

    // Ensures deriving a public key from a private key string matches the result from the corresponding PrivateKey.
    @Test
    public void deriveFromPrivateKeyString() {
        PrivateKey privateKey = PrivateKey.generateRandom();
        String privateKeyString = privateKey.toString();
        PublicKey expected = PrivateKey.derivePublicKey(privateKey);
        PublicKey actual = PublicKey.derivePublicKey(privateKeyString);
        assertEquals(expected, actual);
    }
}
