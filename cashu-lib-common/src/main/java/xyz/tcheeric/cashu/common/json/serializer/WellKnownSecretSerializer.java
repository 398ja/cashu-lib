package xyz.tcheeric.cashu.common.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.WellKnownSecret;

import java.io.IOException;
import java.util.List;

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

        // Element 2: nonce (string)
        gen.writeString(value.getNonce());

        // Element 3: tags (array of tag arrays)
        gen.writeStartArray();
        if (value.getTags() != null) {
            for (WellKnownSecret.Tag tag : value.getTags()) {
                gen.writeStartArray();
                gen.writeString(tag.getKey());
                if (tag.getValues() != null) {
                    for (Object v : tag.getValues()) {
                        if (v instanceof Number) {
                            gen.writeNumber(((Number) v).longValue());
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
