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

    // Confidential interface tests

    @Test
    public void testGetValue_ReturnsFullValue() {
        PrivateKey priv = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");
        String value = priv.getValue();

        assertEquals("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b", value);
    }

    @Test
    public void testDisplay_HidesValueWithMoreThan3Characters() {
        PrivateKey priv = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");
        String displayed = priv.display();

        assertEquals("...f9b", displayed);
        assertNotEquals(priv.getValue(), displayed);
    }

    @Test
    public void testDisplay_ShowsHiddenMessageForVeryShortValues() {
        // Create a mock implementation to test edge case with short values
        Confidential shortValue = new Confidential() {
            @Override
            public String getValue() {
                return "abc";
            }
        };

        assertEquals("*** hidden ***", shortValue.display());
    }

    @Test
    public void testDisplay_ShowsHiddenMessageForEmptyValues() {
        Confidential emptyValue = new Confidential() {
            @Override
            public String getValue() {
                return "";
            }
        };

        assertEquals("*** hidden ***", emptyValue.display());
    }

    @Test
    public void testDisplay_ShowsLast3CharactersFor4CharacterValue() {
        Confidential fourChars = new Confidential() {
            @Override
            public String getValue() {
                return "abcd";
            }
        };

        assertEquals("...bcd", fourChars.display());
    }

    @Test
    public void testToString_UsesDisplayMethod() {
        PrivateKey priv = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");
        String toString = priv.toString();

        assertEquals(priv.display(), toString);
        assertEquals("...f9b", toString);
        assertNotEquals(priv.getValue(), toString);
    }

    @Test
    public void testConfidentialInterface_PreventsAccidentalLogging() {
        PrivateKey priv = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");

        // When using toString (e.g., in logging), the full value should not be exposed
        String logged = "Private key: " + priv;
        assertFalse(logged.contains("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b"));
        assertTrue(logged.contains("...f9b"));
    }

    @Test
    public void testGetValue_ProvidesAccessToFullValueWhenNeeded() {
        PrivateKey priv = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");

        // getValue should allow intentional access to the full value
        String fullValue = priv.getValue();
        assertEquals(64, fullValue.length());
        assertTrue(fullValue.matches("[0-9a-f]{64}"));
    }
}

