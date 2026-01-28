package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.BlindSignature;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.Signature;
import xyz.tcheeric.cashu.common.nut12.DLEQProof;

import java.io.IOException;

public class BlindSignatureDeserializer extends JsonDeserializer<BlindSignature> {
    @Override
    public BlindSignature deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            int amount = node.get("amount").asInt();
            JsonNode idNode = node.get("id");
            KeysetId keysetId = null;
            if (idNode != null && !idNode.isNull()) {
                keysetId = KeysetId.fromString(idNode.asText());
            }
            Signature blindedSignature = mapper.treeToValue(node.get("C_"), Signature.class);
            DLEQProof dleqProof = null;
            if (node.hasNonNull("dleq")) {
                dleqProof = mapper.treeToValue(node.get("dleq"), DLEQProof.class);
            }
            return BlindSignature.builder()
                    .amount(amount)
                    .keySetId(keysetId)
                    .blindedSignature(blindedSignature)
                    .dleq(dleqProof)
                    .build();
        }
        throw new RuntimeException("Invalid BlindSignature format");
    }
}
