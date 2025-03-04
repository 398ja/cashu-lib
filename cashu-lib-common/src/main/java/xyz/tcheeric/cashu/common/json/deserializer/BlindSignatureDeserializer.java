package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.common.BlindSignature;
import xyz.tcheeric.cashu.common.codec.impl.BlindSignatudeDecoder;

import java.io.IOException;

public class BlindSignatureDeserializer extends JsonDeserializer<BlindSignature> {
    @Override
    public BlindSignature deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            BlindSignatudeDecoder decoder = new BlindSignatudeDecoder(node.toString());
            return decoder.decode();
        }
        throw new RuntimeException("Invalid BlindSignature format");
    }
}
