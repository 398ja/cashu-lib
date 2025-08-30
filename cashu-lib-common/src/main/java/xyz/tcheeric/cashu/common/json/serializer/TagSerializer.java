package xyz.tcheeric.cashu.common.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import xyz.tcheeric.cashu.common.P2PKSecret;
import xyz.tcheeric.cashu.common.WellKnownSecret;

import java.io.IOException;

public class TagSerializer extends JsonSerializer<WellKnownSecret.Tag> {
    @Override
    public void serialize(WellKnownSecret.Tag value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();
        gen.writeString(value.getKey());
        if (value.getValues() != null) {
            for (Object v : value.getValues()) {
                if (v instanceof String s) {
                    gen.writeString(s);
                } else if (v instanceof Integer i) {
                    gen.writeNumber(i);
                } else if (v instanceof P2PKSecret.SignatureFlag f) {
                    gen.writeString(f.name());
                } else {
                    gen.writeString(String.valueOf(v));
                }
            }
        }
        gen.writeEndArray();
    }
}

