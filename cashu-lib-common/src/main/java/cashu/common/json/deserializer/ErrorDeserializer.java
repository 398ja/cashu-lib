package cashu.common.json.deserializer;

import cashu.common.json.codec.impl.ErrorDecoder;
import cashu.common.util.Error;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

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
