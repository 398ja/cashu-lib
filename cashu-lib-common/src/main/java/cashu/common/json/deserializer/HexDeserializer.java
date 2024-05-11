package cashu.common.json.deserializer;

import cashu.common.json.codec.impl.HexDecoder;
import cashu.common.model.Hex;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class HexDeserializer extends JsonDeserializer<Hex> {
    @Override
    public Hex deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = p.readValueAsTree();
        if (node.isTextual()) {
            return new HexDecoder(node.textValue()).decode();
        }
        throw new RuntimeException("Invalid Hex format");
    }
}
