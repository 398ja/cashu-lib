package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import xyz.tcheeric.cashu.common.Signature;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignatureJsonDeserializerTest {

    private final SignatureJsonDeserializer deserializer = new SignatureJsonDeserializer();

    private Signature deserialize(String value) throws IOException {
        try (JsonParser parser = new JsonFactory().createParser("\"" + value + "\"")) {
            parser.nextToken();
            return deserializer.deserialize(parser, null);
        }
    }

    @Test
    /**
     * Ensures a 66-character compressed signature is returned unchanged by the deserializer.
     */
    void shouldKeepCompressedSignatureUnchanged() throws Exception {
        // Arrange
        String compressedSignature = "02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea";

        // Act
        Signature parsedSignature = deserialize(compressedSignature);

        // Assert
        assertEquals(compressedSignature, parsedSignature.toString());
    }

    @Test
    /**
     * Ensures a 64-character x-only signature is normalized by prefixing the even y-indicator.
     */
    void shouldPrefixXOnlySignatureWithEvenIndicator() throws Exception {
        // Arrange
        String xOnlySignature = "1111111111111111111111111111111111111111111111111111111111111111";

        // Act
        Signature parsedSignature = deserialize(xOnlySignature);

        // Assert
        assertEquals("02" + xOnlySignature, parsedSignature.toString());
    }

    @Test
    /**
     * Ensures a 128-character Schnorr signature keeps the leading 64 characters as the x-coordinate.
     */
    void shouldPreserveLeadingHalfOfFullSchnorrSignature() throws Exception {
        // Arrange
        String rComponent = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String sComponent = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
        String schnorrSignature = rComponent + sComponent;

        // Act
        Signature parsedSignature = deserialize(schnorrSignature);

        // Assert
        assertEquals("02" + rComponent, parsedSignature.toString());
    }

    @Test
    /**
     * Ensures unsupported longer inputs throw to avoid silently corrupting unexpected signatures.
     */
    void shouldRejectUnexpectedlyLongInputs() {
        // Arrange
        String invalidSegment = "001122";
        Executable deserializeAction = () -> deserialize(invalidSegment.repeat(20));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, deserializeAction);
    }
}
