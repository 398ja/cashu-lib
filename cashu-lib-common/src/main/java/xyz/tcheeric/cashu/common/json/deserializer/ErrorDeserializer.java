package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.util.Error;

import java.io.IOException;

public class ErrorDeserializer extends JsonDeserializer<Error> {
    @Override
    public Error deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            return mapper.readValue(node.toString(), Error.class);
        }
        throw new RuntimeException("Invalid Error format");
    }
}
