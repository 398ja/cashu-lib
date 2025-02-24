package xyz.tcheeric.cashu.common.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.TokenV3;

import java.io.IOException;

public class TokenV3Serializer<T extends Secret> extends JsonSerializer<TokenV3<T>> {
    @Override
    public void serialize(TokenV3<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeObjectField("token", value.getMintProofs());
        gen.writeStringField("unit", value.getUnit());
        gen.writeStringField("memo", value.getMemo());

        gen.writeEndObject();
    }
}
