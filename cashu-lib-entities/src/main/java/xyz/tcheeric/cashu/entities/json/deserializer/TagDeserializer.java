package xyz.tcheeric.cashu.entities.json.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.entities.P2PKSecret;
import xyz.tcheeric.cashu.entities.WellKnownSecret;

import java.io.IOException;

public class TagDeserializer extends JsonDeserializer<WellKnownSecret.Tag> {
    @Override
    public WellKnownSecret.Tag deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = p.readValueAsTree();
        String key = node.get(0).textValue();
        WellKnownSecret.Tag tag = new WellKnownSecret.Tag(key);

        switch (P2PKSecret.P2PKTag.valueOf(key.toLowerCase())) {
            case P2PKSecret.P2PKTag.sigflag -> tag.addValue(P2PKSecret.SignatureFlag.valueOf(node.get(1).textValue()));
            case P2PKSecret.P2PKTag.pubkeys, P2PKSecret.P2PKTag.refund -> {
                for (int i = 1; i < node.size(); i++) {
                    tag.addValue(node.get(i).textValue());
                }
            }
            case P2PKSecret.P2PKTag.n_sigs, P2PKSecret.P2PKTag.locktime -> tag.addValue(node.get(1).intValue());
            default -> throw new IllegalArgumentException("Invalid tag");
        }

        return tag;
    }
}
