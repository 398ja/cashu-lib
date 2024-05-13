package cashu.common.json.deserializer;

import cashu.common.json.codec.impl.KeySetDecoder;
import cashu.common.model.KeySet;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

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
