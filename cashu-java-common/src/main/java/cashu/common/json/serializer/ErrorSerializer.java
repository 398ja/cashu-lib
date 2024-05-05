package cashu.common.json.serializer;

import cashu.common.error.Error;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class ErrorSerializer extends JsonSerializer<Error> {
    @Override
    public void serialize(Error value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("detail", value.getDetail());
        gen.writeNumberField("code", value.getCode());
        gen.writeEndObject();
    }
}
