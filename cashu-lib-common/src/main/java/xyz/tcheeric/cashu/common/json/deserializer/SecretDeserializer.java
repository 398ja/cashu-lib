package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.WellKnownSecret;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import java.io.IOException;

/**
 * Deserializes Secret from JSON, supporting both RandomStringSecret (hex strings)
 * and WellKnownSecret (NUT-10 format JSON arrays as strings).
 */
public class SecretDeserializer extends JsonDeserializer<Secret> {
    @Override
    public Secret deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isTextual()) {
            // Use SecretUtil.toSecret() which handles both:
            // - Random hex strings (NUT-00): "64charhexstring"
            // - NUT-10 JSON arrays as strings: "[\"VOUCHER\",\"hexdata\",\"nonce\",[]]"
            return SecretUtil.toSecret(node.textValue());
        }
        if (node.isObject()) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            return mapper.treeToValue(node, WellKnownSecret.class);
        }
        if (node.isArray()) {
            // Handle direct JSON array format (not as string)
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            return mapper.treeToValue(node, WellKnownSecret.class);
        }
        throw new RuntimeException("Invalid Secret format: expected string, array, or object with 'kind' field");
    }
}
