package xyz.tcheeric.cashu.entities.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.entities.PrivateKey;

import java.io.IOException;

public class PrivateKeyDeserializer extends JsonDeserializer<PrivateKey> {
    @Override
    public PrivateKey deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isTextual()) {
            return PrivateKey.fromString(node.textValue());
        }
        throw new RuntimeException("Invalid CryptoElement format");
    }
}
