package xyz.tcheeric.cashu.common.json.deserializer;

import xyz.tcheeric.cashu.common.model.Signature;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class SignatureDeserializer extends JsonDeserializer<Signature> {
    @Override
    public Signature deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isTextual()) {
            return Signature.fromString(node.textValue());
        }
        throw new RuntimeException("Invalid CryptoElement format");
    }
}
