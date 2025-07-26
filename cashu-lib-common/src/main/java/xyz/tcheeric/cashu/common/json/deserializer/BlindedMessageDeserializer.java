package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.BlindedMessage;

import java.io.IOException;

public class BlindedMessageDeserializer extends JsonDeserializer<BlindedMessage> {
    @Override
    public BlindedMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            return mapper.readValue(node.toString(), BlindedMessage.class);
        }
        throw new RuntimeException("Invalid BlindedMessage format");
    }
}
