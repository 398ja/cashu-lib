package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.common.Witness;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.io.IOException;

/**
 * Reads a NUT-11 {@code witness}, which the spec transports as a JSON-encoded <em>string</em>
 * rather than as a nested object. Both forms are accepted so a proof produced by any other
 * implementation parses, and a proof produced by this library keeps parsing.
 */
public class WitnessDeserializer extends JsonDeserializer<Witness> {

    @Override
    public Witness deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isTextual() ? fromEncodedString(node.asText()) : fromObject(node);
    }

    private static Witness fromEncodedString(String encoded) {
        if (encoded.isBlank()) {
            return null;
        }
        try {
            return JsonUtils.JSON_MAPPER.readValue(encoded, Witness.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Witness is not valid JSON. Got: " + encoded, e);
        }
    }

    private static Witness fromObject(JsonNode node) {
        return JsonUtils.JSON_MAPPER.convertValue(node, Witness.class);
    }
}
