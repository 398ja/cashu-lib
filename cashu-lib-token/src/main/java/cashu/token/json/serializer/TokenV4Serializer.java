package cashu.token.json.serializer;

import cashu.token.TokenV4;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class TokenV4Serializer extends JsonSerializer<TokenV4> {
    @Override
    public void serialize(TokenV4 value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeObjectField("mintUrl", value.getMintUrl());
        gen.writeStringField("unit", value.getUnit());
        gen.writeStringField("memo", value.getMemo());
        gen.writeObjectField("tokenDataList", value.getTokenDataList());

        gen.writeEndObject();

    }
}
