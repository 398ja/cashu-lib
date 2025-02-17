package xyz.tcheeric.cashu.entities.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.entities.codec.impl.ErrorDecoder;

import java.io.IOException;

public class ErrorDeserializer extends JsonDeserializer<Error> {
    @Override
    public Error deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            ErrorDecoder decoder = new ErrorDecoder(node.toString());
            return decoder.decode();
        }
        throw new RuntimeException("Invalid Error format");
    }
}
