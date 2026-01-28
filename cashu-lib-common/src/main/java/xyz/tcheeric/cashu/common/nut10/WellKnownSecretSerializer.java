package xyz.tcheeric.cashu.common.nut10;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;

/**
 * Serializes WellKnownSecret to NUT-10 array format: ["kind", "hexdata", "nonce", [[tag arrays]]]
 *
 * <p>See: <a href="https://github.com/cashubtc/nuts/blob/main/10.md">NUT-10</a>
 */
public class WellKnownSecretSerializer extends JsonSerializer<WellKnownSecret> {

    @Override
    public void serialize(WellKnownSecret value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();

        // Element 0: kind (string)
        gen.writeString(value.getKind().name());

        // Element 1: data (hex-encoded bytes)
        gen.writeString(Hex.toHexString(value.getData()));

        // Element 2: nonce (string or null)
        // Note: writeString(null) outputs "null" (string), so we must explicitly write JSON null
        if (value.getNonce() == null) {
            gen.writeNull();
        } else {
            gen.writeString(value.getNonce());
        }

        // Element 3: tags (array of tag arrays)
        gen.writeStartArray();
        if (value.getTags() != null) {
            for (WellKnownSecret.Tag tag : value.getTags()) {
                gen.writeStartArray();
                gen.writeString(tag.getKey());
                if (tag.getValues() != null) {
                    for (Object v : tag.getValues()) {
                        if (v instanceof Number n) {
                            // Preserve fractional values for doubles/floats
                            if (v instanceof Double || v instanceof Float) {
                                double d = n.doubleValue();
                                if (d != Math.floor(d)) {
                                    gen.writeNumber(d);
                                } else {
                                    gen.writeNumber(n.longValue());
                                }
                            } else {
                                gen.writeNumber(n.longValue());
                            }
                        } else {
                            gen.writeString(String.valueOf(v));
                        }
                    }
                }
                gen.writeEndArray();
            }
        }
        gen.writeEndArray();

        gen.writeEndArray();
    }
}
