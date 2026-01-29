package xyz.tcheeric.cashu.common.nut10;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;
import xyz.tcheeric.cashu.common.nut18.VoucherSecret;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deserializes WellKnownSecret supporting two formats:
 * <ul>
 *   <li>NUT-10 array format: ["kind", "hexdata", "nonce", [[tag arrays]]]</li>
 *   <li>Legacy object format: ["kind", {"nonce": "...", "data": "...", "tags": [...]}]</li>
 * </ul>
 *
 * <p>See: <a href="https://github.com/cashubtc/nuts/blob/main/10.md">NUT-10</a>
 */
@Slf4j
public class WellKnownSecretDeserializer extends JsonDeserializer<WellKnownSecret> {
    @Override
    public WellKnownSecret deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        // Handle object format (from SecretUtil.convertValue with Map)
        if (node.isObject()) {
            return deserializeObjectFormat(node);
        }

        // Handle array format
        if (!node.isArray() || node.size() < 2) {
            throw new IOException("Invalid secret format: expected array with at least 2 elements");
        }

        // Element 0: kind (string)
        String kindStr = node.get(0).asText();
        WellKnownSecret.Kind kind = WellKnownSecret.Kind.valueOf(kindStr);

        // Check if this is legacy format (2-element with object) or NUT-10 format (4-element)
        JsonNode secondElement = node.get(1);
        if (secondElement.isObject()) {
            // Legacy format: ["kind", {object}]
            return deserializeLegacyFormat(kind, secondElement);
        } else if (node.size() >= 4) {
            // NUT-10 format: ["kind", "hexdata", "nonce", [tags]]
            return deserializeNut10Format(kind, node);
        } else {
            throw new IOException("Invalid secret format: expected NUT-10 (4-element) or legacy (2-element with object)");
        }
    }

    /**
     * Deserializes object format: {"kind": "...", "nonce": "...", "data": "...", "tags": [...]}
     * Used when SecretUtil.convertValue passes a Map.
     */
    private WellKnownSecret deserializeObjectFormat(JsonNode objectNode) {
        JsonNode kindNode = objectNode.get("kind");
        if (kindNode == null) {
            throw new IllegalArgumentException("Missing 'kind' field in secret object");
        }
        WellKnownSecret.Kind kind = WellKnownSecret.Kind.valueOf(kindNode.asText());
        return deserializeLegacyFormat(kind, objectNode);
    }

    /**
     * Deserializes NUT-10 format: ["kind", "hexdata", "nonce", [tags]]
     */
    private WellKnownSecret deserializeNut10Format(WellKnownSecret.Kind kind, JsonNode node) {
        // Element 1: data (hex-encoded bytes)
        String dataHex = node.get(1).asText();
        byte[] data = Hex.decode(dataHex);

        // Element 2: nonce (string or null)
        // Note: JsonNode.asText() returns "null" (string) for null nodes, so we must check explicitly
        JsonNode nonceNode = node.get(2);
        String nonce = nonceNode.isNull() ? null : nonceNode.asText();

        // Create the appropriate secret type
        WellKnownSecret secret = createSecret(kind);
        secret.setData(data);
        secret.setNonce(nonce);

        // Element 3: tags (array of tag arrays)
        JsonNode tagsNode = node.get(3);
        if (tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                if (tagNode.isArray() && tagNode.size() >= 1) {
                    String key = tagNode.get(0).asText();
                    WellKnownSecret.Tag tag = new WellKnownSecret.Tag(key);
                    for (int i = 1; i < tagNode.size(); i++) {
                        JsonNode valueNode = tagNode.get(i);
                        if (valueNode.isNumber()) {
                            // Preserve fractional values
                            if (valueNode.isFloatingPointNumber()) {
                                tag.addValue(valueNode.doubleValue());
                            } else {
                                tag.addValue(valueNode.longValue());
                            }
                        } else {
                            tag.addValue(valueNode.asText());
                        }
                    }
                    if (kind == WellKnownSecret.Kind.P2PK) {
                        convertP2PKTagValues(tag);
                    }
                    secret.addTag(tag);
                }
            }
        }

        return secret;
    }

    /**
     * Deserializes legacy format: ["kind", {"nonce": "...", "data": "...", "tags": [...]}]
     */
    private WellKnownSecret deserializeLegacyFormat(WellKnownSecret.Kind kind, JsonNode objectNode) {
        WellKnownSecret secret = createSecret(kind);

        // Extract nonce (check for both missing and null nodes)
        JsonNode nonceNode = objectNode.get("nonce");
        if (nonceNode != null && !nonceNode.isNull()) {
            secret.setNonce(nonceNode.asText());
        }

        // Extract data (hex-encoded)
        JsonNode dataNode = objectNode.get("data");
        if (dataNode != null) {
            secret.setData(Hex.decode(dataNode.asText()));
        }

        // Extract tags
        JsonNode tagsNode = objectNode.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                if (tagNode.isArray() && tagNode.size() >= 1) {
                    String key = tagNode.get(0).asText();
                    WellKnownSecret.Tag tag = new WellKnownSecret.Tag(key);
                    for (int i = 1; i < tagNode.size(); i++) {
                        JsonNode valueNode = tagNode.get(i);
                        if (valueNode.isNumber()) {
                            // Preserve fractional values
                            if (valueNode.isFloatingPointNumber()) {
                                tag.addValue(valueNode.doubleValue());
                            } else {
                                tag.addValue(valueNode.longValue());
                            }
                        } else {
                            tag.addValue(valueNode.asText());
                        }
                    }
                    if (kind == WellKnownSecret.Kind.P2PK) {
                        convertP2PKTagValues(tag);
                    }
                    secret.addTag(tag);
                }
            }
        }

        return secret;
    }

    private WellKnownSecret createSecret(WellKnownSecret.Kind kind) {
        return switch (kind) {
            case P2PK -> new P2PKSecret();
            case VOUCHER -> new VoucherSecret();
            default -> throw new IllegalArgumentException("Invalid kind: " + kind);
        };
    }

    private void convertP2PKTagValues(WellKnownSecret.Tag tag) {
        switch (tag.getKey()) {
            case "sigflag" -> {
                List<Object> values = new ArrayList<>();
                for (Object v : tag.getValues()) {
                    if (v instanceof String s) {
                        values.add(P2PKSecret.SignatureFlag.valueOf(s));
                    } else {
                        values.add(v);
                    }
                }
                tag.setValues(values);
            }
            case "n_sigs", "locktime" -> {
                List<Object> values = new ArrayList<>();
                for (Object v : tag.getValues()) {
                    if (v instanceof Number n) {
                        values.add(n.intValue());
                    } else {
                        values.add(v);
                    }
                }
                tag.setValues(values);
            }
            case "pubkeys", "refund" -> {
                List<Object> values = new ArrayList<>();
                for (Object v : tag.getValues()) {
                    values.add(String.valueOf(v));
                }
                tag.setValues(values);
            }
        }
    }
}
