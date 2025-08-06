package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.WellKnownSecret;

import java.io.IOException;

public class SecretDeserializer extends JsonDeserializer<Secret> {
    @Override
    public Secret deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isTextual()) {
            return RandomStringSecret.fromString(node.textValue());
        }
        if (node.isObject()) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            return mapper.treeToValue(node, WellKnownSecret.class);
        }
        throw new RuntimeException("Invalid Secret format: expected string or object with 'kind' field");
    }
}
