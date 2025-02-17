package xyz.tcheeric.cashu.entities.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import xyz.tcheeric.cashu.entities.BlindedMessage;

import java.io.IOException;

public class BlindedMessageSerializer extends JsonSerializer<BlindedMessage> {
    @Override
    public void serialize(BlindedMessage value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("amount", value.getAmount());
        gen.writeStringField("id", value.getKeySetId());
        gen.writeStringField("B_", value.getBlindedMessage().toString());
        gen.writeEndObject();
    }
}
