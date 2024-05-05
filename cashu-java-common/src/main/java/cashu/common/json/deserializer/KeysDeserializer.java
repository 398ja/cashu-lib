package cashu.common.json.deserializer;

import cashu.common.model.Keys;
import cashu.common.json.codec.impl.KeysDecoder;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class KeysDeserializer extends JsonDeserializer<Keys> {
    @Override
    public Keys deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            KeysDecoder decoder = new KeysDecoder(node.toString());
            return decoder.decode();
        }
        throw new RuntimeException("Invalid Keys format");
    }
}
