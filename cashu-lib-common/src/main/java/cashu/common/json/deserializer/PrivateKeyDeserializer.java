package cashu.common.json.deserializer;

import cashu.common.model.PrivateKey;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class PrivateKeyDeserializer extends JsonDeserializer<PrivateKey> {
    @Override
    public PrivateKey deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isTextual()) {
            return PrivateKey.fromString(node.textValue());
        }
        throw new RuntimeException("Invalid Hex format");
    }
}
