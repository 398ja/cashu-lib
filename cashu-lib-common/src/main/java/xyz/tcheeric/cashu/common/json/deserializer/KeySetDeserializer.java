package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.common.KeySet;
import xyz.tcheeric.cashu.common.codec.impl.KeySetDecoder;

import java.io.IOException;

public class KeySetDeserializer extends JsonDeserializer<KeySet> {

    @Override
    public KeySet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            KeySetDecoder decoder = new KeySetDecoder(node.toString());
            return decoder.decode();
        }
        throw new RuntimeException("Invalid Keys format");
    }
}
