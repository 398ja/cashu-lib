package cashu.common.json.serializer;

import cashu.common.model.KeySet;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class KeySetSerializer extends JsonSerializer<KeySet> {
    @Override
    public void serialize(KeySet value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("id", value.getId());
        gen.writeStringField("unit", value.getUnit());
        gen.writeObjectFieldStart("keys");
        gen.writeObject(value.getKeys());
        gen.writeEndObject();
    }
}
