package cashu.common.json.serializer;

import cashu.common.model.Token;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class TokenSerializer extends JsonSerializer<Token> {
    @Override
    public void serialize(Token value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeObjectField("token", value.getMintProofs());
        gen.writeStringField("unit", value.getUnit());
        gen.writeStringField("memo", value.getMemo());

        gen.writeEndObject();
    }
}
