package cashu.common.json.deserializer;

import cashu.common.model.Proof;
import cashu.common.json.codec.impl.ProofDecoder;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class ProofDeserializer extends JsonDeserializer<Proof> {
    @Override
    public Proof deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            ProofDecoder decoder = new ProofDecoder(node.toString());
            return decoder.decode();
        }
        throw new RuntimeException("Invalid Proof format");
    }
}
