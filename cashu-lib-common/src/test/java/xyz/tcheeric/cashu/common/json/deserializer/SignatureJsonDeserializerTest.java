package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.Signature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SignatureJsonDeserializerTest {

    private final SignatureJsonDeserializer deserializer = new SignatureJsonDeserializer();

    private Signature deserialize(String value) throws IOException {
        try (JsonParser parser = new JsonFactory().createParser("\"" + value + "\"")) {
            parser.nextToken();
            return deserializer.deserialize(parser, null);
        }
    }

    // Ensures a 66-character compressed signature is returned unchanged by the deserializer
    @Test
    public void deserializeKeepsCompressedSignatureUnchanged() throws Exception {
        String compressed = "02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea";
        Signature signature = deserialize(compressed);
        assertEquals(compressed, signature.toString());
    }

    // Ensures a 64-character x-only signature is normalized by prefixing the even y-indicator
    @Test
    public void deserializePrefixesXOnlySignature() throws Exception {
        String xOnly = "1111111111111111111111111111111111111111111111111111111111111111";
        Signature signature = deserialize(xOnly);
        assertEquals("02" + xOnly, signature.toString());
    }

    // Ensures a 128-character Schnorr signature keeps the leading 64 characters as the x-coordinate
    @Test
    public void deserializeKeepsLeadingHalfOfFullSchnorrSignature() throws Exception {
        String r = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String s = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
        Signature signature = deserialize(r + s);
        assertEquals("02" + r, signature.toString());
    }

    // Ensures unsupported longer inputs throw to avoid silently corrupting unexpected signatures
    @Test
    public void deserializeRejectsUnexpectedLongInputs() {
        String invalid = "001122";
        assertThrows(IllegalArgumentException.class, () -> deserialize(invalid.repeat(20)));
    }
}
