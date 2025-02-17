package xyz.tcheeric.cashu.entities.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import xyz.tcheeric.cashu.entities.TokenV3;

import java.io.IOException;

public class TokenV3Serializer extends JsonSerializer<TokenV3> {
    @Override
    public void serialize(TokenV3 value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeObjectField("token", value.getMintProofs());
        gen.writeStringField("unit", value.getUnit());
        gen.writeStringField("memo", value.getMemo());

        gen.writeEndObject();
    }
}
