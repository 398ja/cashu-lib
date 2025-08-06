package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.Witness;

import java.io.IOException;

public class BlindedMessageDeserializer extends JsonDeserializer<BlindedMessage> {
    @Override
    public BlindedMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            int amount = node.get("amount").asInt();
            JsonNode idNode = node.get("id");
            KeysetId keysetId = null;
            if (idNode != null && !idNode.isNull()) {
                keysetId = KeysetId.fromString(idNode.asText());
            }
            PublicKey blindedMessage = mapper.treeToValue(node.get("B_"), PublicKey.class);
            JsonNode witnessNode = node.get("witness");
            Witness witness = null;
            if (witnessNode != null && !witnessNode.isNull()) {
                witness = mapper.treeToValue(witnessNode, Witness.class);
            }
            return BlindedMessage.builder()
                    .amount(amount)
                    .keySetId(keysetId)
                    .blindedMessage(blindedMessage)
                    .witness(witness)
                    .build();
        }
        throw new RuntimeException("Invalid BlindedMessage format");
    }
}
