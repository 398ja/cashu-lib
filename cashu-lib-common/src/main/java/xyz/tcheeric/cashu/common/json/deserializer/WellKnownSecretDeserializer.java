package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.model.P2PKSecret;
import xyz.tcheeric.cashu.common.model.PublicKey;
import xyz.tcheeric.cashu.common.model.WellKnownSecret;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WellKnownSecretDeserializer extends JsonDeserializer<WellKnownSecret> {
    @Override
    public WellKnownSecret deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = p.readValueAsTree();
        if (node.isArray()) {

            // [kind, data]
            WellKnownSecret.Kind kind = WellKnownSecret.Kind.valueOf(node.get(0).textValue());
            JsonNode dataNode = node.get(1);
            byte[] data = Hex.decode(dataNode.get("data").textValue());

            // P2PK
            WellKnownSecret secret = kind.equals(WellKnownSecret.Kind.P2PK) ? new P2PKSecret(data) : null;
            if (secret == null) {
                throw new IllegalArgumentException("Invalid kind");
            }

            // nonce
            String nonce = dataNode.get("nonce").textValue();
            secret.setNonce(nonce);

            // tags
            JsonNode tagsNode = dataNode.get("tags");
            if (tagsNode.isArray()) {
                for (JsonNode tagNode : tagsNode) {
                    WellKnownSecret.Tag tag = new ObjectMapper().readValue(tagNode.toString(), WellKnownSecret.Tag.class);
                    secret.addTag(tag);
                }
            }
            return secret;
        }
        throw new RuntimeException("Invalid CryptoElement format");
    }
}
