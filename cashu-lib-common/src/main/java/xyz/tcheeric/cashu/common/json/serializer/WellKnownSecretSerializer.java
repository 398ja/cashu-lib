package xyz.tcheeric.cashu.common.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.model.WellKnownSecret;

import java.io.IOException;

public class WellKnownSecretSerializer extends JsonSerializer<WellKnownSecret> {

    @Override
    public void serialize(WellKnownSecret value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();
        gen.writeString(value.getKind().name());

        gen.writeStartObject();
        gen.writeStringField("nonce", value.getNonce());
        gen.writeStringField("data", Hex.toHexString(value.getData()));

        gen.writeFieldName("tags");
        gen.writeStartArray();
        for (WellKnownSecret.Tag tag : value.getTags()) {
            gen.writeStartArray();
            gen.writeString(tag.getKey());
            for (Object tagValue : tag.getValues()) {
                gen.writeObject(tagValue.toString());
            }
            gen.writeEndArray();
        }
        gen.writeEndArray();

        gen.writeEndObject();
        gen.writeEndArray();
    }
}
