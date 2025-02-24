package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.codec.impl.BlindedMessageDecoder;

import java.io.IOException;

public class BlindedMessageDeserializer extends JsonDeserializer<BlindedMessage> {
    @Override
    public BlindedMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            BlindedMessageDecoder decoder = new BlindedMessageDecoder(node.toString());
            return decoder.decode();
        }
        throw new RuntimeException("Invalid BlindedMessage format");
    }
}
