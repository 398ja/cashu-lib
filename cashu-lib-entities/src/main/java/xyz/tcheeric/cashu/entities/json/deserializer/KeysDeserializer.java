package xyz.tcheeric.cashu.entities.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.entities.Keys;
import xyz.tcheeric.cashu.entities.codec.impl.KeysDecoder;

import java.io.IOException;

public class KeysDeserializer extends JsonDeserializer<Keys> {
    @Override
    public Keys deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            KeysDecoder decoder = new KeysDecoder(node.toString());
            return decoder.decode();
        }
        throw new RuntimeException("Invalid Keys format");
    }
}
