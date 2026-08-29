package xyz.tcheeric.cashu.common.nut10;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;

/**
 * Encodes a {@link WellKnownSecret} in the NUT-10 well-known secret format:
 * {@code ["kind", {"nonce": ..., "data": ..., "tags": [["key", "value", ...], ...]}]}.
 *
 * <p>Two elements, the second an object. An earlier revision of this class flattened the object
 * into three further array elements; see
 * {@code docs/explanation/adr/0003-nut10-secret-serialization.md} for why that was wrong and what
 * changes as a result.
 *
 * <p>This encoding is used for secrets this library <em>constructs</em>. A secret that was parsed
 * from the wire replays its original string instead and never reaches here — see
 * {@link WellKnownSecret#toString()}.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/10.md">NUT-10</a>
 */
public class WellKnownSecretSerializer extends JsonSerializer<WellKnownSecret> {

    @Override
    public void serialize(WellKnownSecret value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();
        gen.writeString(value.getKind().name());
        writeConditionObject(value, gen);
        gen.writeEndArray();
    }

    /**
     * Writes NUT-10's second element. Key order is {@code nonce}, {@code data}, {@code tags}, as
     * the spec prints it: the encoding is a signed and hashed message, so a stable order is part of
     * the wire format rather than a cosmetic choice.
     */
    private void writeConditionObject(WellKnownSecret value, JsonGenerator gen) throws IOException {
        gen.writeStartObject();

        if (value.getNonce() == null) {
            // writeString(null) emits the four-character string "null", which is a different secret.
            gen.writeNullField("nonce");
        } else {
            gen.writeStringField("nonce", value.getNonce());
        }

        gen.writeStringField("data", value.getData() == null ? "" : Hex.toHexString(value.getData()));

        gen.writeFieldName("tags");
        writeTags(value, gen);

        gen.writeEndObject();
    }

    private void writeTags(WellKnownSecret value, JsonGenerator gen) throws IOException {
        gen.writeStartArray();
        if (value.getTags() != null) {
            for (WellKnownSecret.Tag tag : value.getTags()) {
                writeTag(tag, gen);
            }
        }
        gen.writeEndArray();
    }

    /**
     * NUT-10 requires every tag element to be a string, and NUT-11 notes that wallets must cast the
     * integer-valued tags accordingly, so a numeric value is written as its decimal string rather
     * than as a JSON number.
     */
    private void writeTag(WellKnownSecret.Tag tag, JsonGenerator gen) throws IOException {
        gen.writeStartArray();
        gen.writeString(tag.getKey());
        if (tag.getValues() != null) {
            for (Object value : tag.getValues()) {
                gen.writeString(asTagString(value));
            }
        }
        gen.writeEndArray();
    }

    private static String asTagString(Object value) {
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            return d == Math.floor(d) && !Double.isInfinite(d)
                    ? String.valueOf((long) d)
                    : String.valueOf(d);
        }
        if (value instanceof Number n) {
            return String.valueOf(n.longValue());
        }
        return String.valueOf(value);
    }
}
