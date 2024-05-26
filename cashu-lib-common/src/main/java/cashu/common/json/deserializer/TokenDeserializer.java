package cashu.common.json.deserializer;

import cashu.common.json.codec.impl.TokenDecoder;
import cashu.common.model.Token;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class TokenDeserializer  extends JsonDeserializer<Token> {
    @Override
    public Token deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            TokenDecoder decoder = new TokenDecoder(node.toString());
            return decoder.decode();
        }
        throw new RuntimeException("Invalid Token format");
    }
}
