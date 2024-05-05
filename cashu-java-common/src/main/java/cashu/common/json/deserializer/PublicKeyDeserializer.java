package cashu.common.json.deserializer;

import cashu.common.model.PrivateKey;
import cashu.common.model.PublicKey;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class PublicKeyDeserializer extends JsonDeserializer<PublicKey> {
    @Override
    public PublicKey deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = p.readValueAsTree();
        if (node.isTextual()) {
            return PublicKey.fromString(node.textValue());
        }
        throw new RuntimeException("Invalid Hex format");
    }
}