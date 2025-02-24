package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.common.PrivateKey;

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
