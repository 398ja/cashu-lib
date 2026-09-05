package xyz.tcheeric.cashu.common.nut10;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;
import xyz.tcheeric.cashu.common.nut11.P2PKVoucherSecret;
import xyz.tcheeric.cashu.common.nut18.VoucherSecret;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a {@link WellKnownSecret} from either encoding this library has emitted:
 * <ul>
 *   <li>the NUT-10 form, {@code ["kind", {"nonce": ..., "data": ..., "tags": [...]}]}</li>
 *   <li>the flattened form, {@code ["kind", "hexdata", "nonce", [[tag arrays]]]}, which releases up
 *       to 0.23.0 emitted and which no other implementation understands</li>
 * </ul>
 *
 * <p>Both are accepted so that proofs already issued under the flattened form stay readable; only
 * the NUT-10 form is ever written. See
 * {@code docs/explanation/adr/0003-nut10-secret-serialization.md}.
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

        JsonNode secondElement = node.get(1);
        if (secondElement.isObject()) {
            return deserializeSpecFormat(kind, secondElement);
        } else if (node.size() >= 4) {
            return deserializeFlattenedFormat(kind, node);
        } else {
            throw new IOException("Invalid secret format: expected the NUT-10 form [kind, {object}]"
                    + " or the pre-0.24.0 flattened form [kind, data, nonce, tags], got " + node);
        }
    }

    /**
     * Reads the bare-object shape {@code {"kind": ..., "nonce": ..., "data": ..., "tags": [...]}},
     * which is not a wire format but is what an in-process {@code Map} conversion produces.
     */
    private WellKnownSecret deserializeObjectFormat(JsonNode objectNode) {
        JsonNode kindNode = objectNode.get("kind");
        if (kindNode == null) {
            throw new IllegalArgumentException("Missing 'kind' field in secret object");
        }
        WellKnownSecret.Kind kind = WellKnownSecret.Kind.valueOf(kindNode.asText());
        return deserializeSpecFormat(kind, objectNode);
    }

    /**
     * Reads the flattened form {@code ["kind", "hexdata", "nonce", [tags]]} that releases up to
     * 0.23.0 emitted, so their proofs remain readable.
     */
    private WellKnownSecret deserializeFlattenedFormat(WellKnownSecret.Kind kind, JsonNode node) {
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
                                // NaN and Infinity are representable in JSON only by extension,
                                // and neither is a meaningful tag value. SecretUtil already
                                // rejected them on its parse path; this one did not, so the two
                                // ingresses disagreed about what a valid secret was (audit L-9).
                                double d = valueNode.doubleValue();
                                if (Double.isNaN(d) || Double.isInfinite(d)) {
                                    throw new IllegalArgumentException(
                                            "Invalid floating-point value in tag '" + key
                                                    + "': NaN or Infinity not allowed");
                                }
                                tag.addValue(d);
                            } else {
                                tag.addValue(valueNode.longValue());
                            }
                        } else {
                            tag.addValue(valueNode.asText());
                        }
                    }
                    if (carriesP2PKTags(kind)) {
                        convertP2PKTagValues(tag);
                    }
                    secret.addTag(tag);
                }
            }
        }

        return validated(secret);
    }

    /**
     * Reads the NUT-10 form {@code ["kind", {"nonce": ..., "data": ..., "tags": [...]}]}.
     */
    private WellKnownSecret deserializeSpecFormat(WellKnownSecret.Kind kind, JsonNode objectNode) {
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
                                // NaN and Infinity are representable in JSON only by extension,
                                // and neither is a meaningful tag value. SecretUtil already
                                // rejected them on its parse path; this one did not, so the two
                                // ingresses disagreed about what a valid secret was (audit L-9).
                                double d = valueNode.doubleValue();
                                if (Double.isNaN(d) || Double.isInfinite(d)) {
                                    throw new IllegalArgumentException(
                                            "Invalid floating-point value in tag '" + key
                                                    + "': NaN or Infinity not allowed");
                                }
                                tag.addValue(d);
                            } else {
                                tag.addValue(valueNode.longValue());
                            }
                        } else {
                            tag.addValue(valueNode.asText());
                        }
                    }
                    if (carriesP2PKTags(kind)) {
                        convertP2PKTagValues(tag);
                    }
                    secret.addTag(tag);
                }
            }
        }

        return validated(secret);
    }

    /**
     * Deserialization is the enforcement point for NUT-11: a malformed P2PK secret must never
     * become a live object.
     *
     * <p>The thrown {@code MalformedP2PKSecretException} is an {@link IllegalArgumentException},
     * so callers that already handle bad input keep working. A mint should catch it specifically
     * and map it to the protocol's unspendable-proof error rather than let it surface as a server
     * fault — NUT-11 frames these conditions as rejection, not as a parse crash.
     */
    private WellKnownSecret validated(WellKnownSecret secret) {
        // P2PKVoucherSecret extends P2PKSecret, so it is validated here too: its lock is a
        // real NUT-11 lock and must meet the same malformed-secret rules. The voucher half —
        // issuer signature, expiry — is domain policy and is checked where that policy lives.
        if (secret instanceof P2PKSecret) {
            ((P2PKSecret) secret).validate();
        }
        return secret;
    }

    /**
     * Whether a kind's tags follow NUT-11's conventions and so need the same normalisation.
     *
     * <p>{@code P2PK_VOUCHER} carries P2PK tags alongside its voucher ones. Without this a
     * parsed secret would hold values of a different type from an identically constructed one,
     * and the two would compare unequal.
     */
    private static boolean carriesP2PKTags(WellKnownSecret.Kind kind) {
        return kind == WellKnownSecret.Kind.P2PK || kind == WellKnownSecret.Kind.P2PK_VOUCHER;
    }

    private WellKnownSecret createSecret(WellKnownSecret.Kind kind) {
        return switch (kind) {
            case P2PK -> new P2PKSecret();
            case P2PK_VOUCHER -> new P2PKVoucherSecret();
            case VOUCHER -> new VoucherSecret();
            default -> throw new IllegalArgumentException("Invalid kind: " + kind);
        };
    }

    /**
     * Normalises a tag's values to strings, which is the only thing NUT-10 tags hold.
     *
     * <p>This used to convert {@code sigflag} to an enum and the counters to ints, so a parsed
     * secret held values of a different type from an identical constructed one and the two were
     * unequal. NUT-11 is explicit that integer-valued tags travel as strings and a wallet casts
     * them on the way in, which {@link P2PKSecret} already does when reading a tag. Keeping the
     * wire type here means one representation everywhere.
     */
    private void convertP2PKTagValues(WellKnownSecret.Tag tag) {
        List<Object> values = new ArrayList<>();
        for (Object value : tag.getValues()) {
            values.add(String.valueOf(value));
        }
        tag.setValues(values);
    }
}
