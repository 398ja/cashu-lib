package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.codec.impl.ProofDecoder;

import java.io.IOException;

public class ProofDeserializer<T extends Secret> extends JsonDeserializer<Proof<T>> {
    @Override
    public Proof<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            ProofDecoder decoder = new ProofDecoder(node.toString());
            return decoder.decode();
        }
        throw new RuntimeException("Invalid Proof format");
    }
}
