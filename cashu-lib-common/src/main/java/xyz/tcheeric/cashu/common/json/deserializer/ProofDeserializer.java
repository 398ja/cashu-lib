package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;

import java.io.IOException;

public class ProofDeserializer<T extends Secret> extends JsonDeserializer<Proof<T>> {
    @Override
    public Proof<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            return mapper.readValue(node.toString(), Proof.class);
        }
        throw new RuntimeException("Invalid Proof format");
    }
}
