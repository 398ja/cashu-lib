package cashu.token.json.serializer;

import cashu.token.TokenV3;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

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
