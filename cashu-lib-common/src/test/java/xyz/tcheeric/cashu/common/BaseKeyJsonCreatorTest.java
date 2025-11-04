package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseKeyJsonCreatorTest {

    @Test
    /**
     * Ensures JSON deserialization maps to PrivateKey correctly.
     */
    void shouldDeserializePrivateKey() throws Exception {
        // Arrange
        ObjectMapper mapper = new ObjectMapper();
        String privateKeyHex = PrivateKey.generateRandom().toString();

        // Act
        PrivateKey deserializedKey = mapper.readValue("\"" + privateKeyHex + "\"", PrivateKey.class);

        // Assert
        assertEquals(PrivateKey.fromString(privateKeyHex), deserializedKey);
    }

    @Test
    /**
     * Ensures JSON deserialization maps to PublicKey correctly.
     */
    void shouldDeserializePublicKey() throws Exception {
        // Arrange
        ObjectMapper mapper = new ObjectMapper();
        String publicKeyHex = PrivateKey.derivePublicKey(PrivateKey.generateRandom()).toString();

        // Act
        PublicKey deserializedKey = mapper.readValue("\"" + publicKeyHex + "\"", PublicKey.class);

        // Assert
        assertEquals(PublicKey.fromString(publicKeyHex, true), deserializedKey);
    }

    @Test
    /**
     * Ensures JSON deserialization maps to Signature correctly.
     */
    void shouldDeserializeSignature() throws Exception {
        // Arrange
        ObjectMapper mapper = new ObjectMapper();
        String signatureHex = "029e8e5050b890a7d6c0968db16bc1d5d5fa040ea1de284f6ec69d61299f671059";

        // Act
        Signature deserializedSignature = mapper.readValue("\"" + signatureHex + "\"", Signature.class);

        // Assert
        assertEquals(Signature.fromString(signatureHex), deserializedSignature);
    }
}
