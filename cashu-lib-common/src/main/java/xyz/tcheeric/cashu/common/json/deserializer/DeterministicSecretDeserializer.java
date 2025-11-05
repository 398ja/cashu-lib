package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.common.DeterministicSecret;

import java.io.IOException;

/**
 * JSON deserializer for {@link DeterministicSecret}.
 *
 * <p>Deserializes hex-encoded strings into DeterministicSecret instances.
 * The deserializer expects a simple text node containing the hex-encoded secret.
 *
 * <p>Note: Metadata (derivation path, keyset ID, counter) is not serialized/deserialized
 * as it's only used locally for debugging and is not part of the protocol.
 *
 * @see DeterministicSecret
 * @author NUT-13 Implementation Team
 * @since 1.0.0
 */
public class DeterministicSecretDeserializer extends JsonDeserializer<DeterministicSecret> {

    @Override
    public DeterministicSecret deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isTextual()) {
            return DeterministicSecret.fromString(node.textValue());
        }
        throw new RuntimeException("Invalid DeterministicSecret format: expected hex string, got " + node.getNodeType());
    }
}
