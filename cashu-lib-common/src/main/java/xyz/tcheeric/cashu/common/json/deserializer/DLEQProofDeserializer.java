package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.common.DLEQProof;

import java.io.IOException;

/**
 * Deserializes DLEQ proofs while preserving optional blinding factors.
 */
public class DLEQProofDeserializer extends JsonDeserializer<DLEQProof> {
    @Override
    public DLEQProof deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node == null || !node.isObject()) {
            throw new RuntimeException("Invalid DLEQProof format");
        }
        String e = textOrNull(node.get("e"));
        String s = textOrNull(node.get("s"));
        String r = textOrNull(node.get("r"));

        if (e == null || s == null) {
            throw new IllegalArgumentException("DLEQ proof must contain e and s scalars");
        }

        if (r == null) {
            return DLEQProof.forBlindSignature(e, s);
        }
        return DLEQProof.forProof(e, s, r);
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
