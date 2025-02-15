package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.java.Log;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.model.P2PKSecret;
import xyz.tcheeric.cashu.common.model.WellKnownSecret;

import java.io.IOException;
import java.util.logging.Level;

@Log
public class WellKnownSecretDeserializer extends JsonDeserializer<WellKnownSecret> {
    @Override
    public WellKnownSecret deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isArray()) {

            // [kind, data]
            WellKnownSecret.Kind kind = WellKnownSecret.Kind.valueOf(node.get(0).textValue());
            JsonNode dataNode = node.get(1);
            byte[] data = Hex.decode(dataNode.get("data").textValue());

            // P2PK
            WellKnownSecret secret = kind.equals(WellKnownSecret.Kind.P2PK) ? new P2PKSecret() : null;
            if (secret == null) {
                throw new IllegalArgumentException("Invalid kind");
            }

            // data
            secret.setData(data);

            // nonce
            String nonce = dataNode.get("nonce").textValue();
            secret.setNonce(nonce);

            // tags
            JsonNode tagsNode = dataNode.get("tags");
            if (tagsNode.isArray()) {
                for (JsonNode tag_node : tagsNode) {
                    if (tag_node.isArray()) {
                        String key = tag_node.get(0).textValue();
                        WellKnownSecret.Tag tag = new WellKnownSecret.Tag();
                        tag.setKey(key);
                        for (int i = 1; i < tag_node.size(); i++) {
                            if (tag_node.get(i).isTextual()) {
                                tag.addValue(tag_node.get(i).textValue());
                            } else if (tag_node.get(i).isNumber()) {
                                tag.addValue(tag_node.get(i).numberValue());
                            } else if (tag_node.get(i).isBoolean()) {
                                tag.addValue(tag_node.get(i).booleanValue());
                            } else {
                                throw new IllegalArgumentException("Invalid tag value");
                            }
                        }
                        secret.addTag(tag);
                    } else {
                        throw new IllegalArgumentException("Invalid tag format");
                    }
                } // for tagsNode
            } else {
                throw new IllegalArgumentException("Invalid tags format");
            }
            return secret;
        }
    log.log(Level.SEVERE, "Invalid CryptoElement format: {0}", node.textValue());
        throw new RuntimeException("Invalid CryptoElement format");
    }
}
