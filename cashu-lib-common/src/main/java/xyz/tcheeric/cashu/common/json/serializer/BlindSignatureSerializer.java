package xyz.tcheeric.cashu.common.json.serializer;

import xyz.tcheeric.cashu.common.model.BlindSignature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class BlindSignatureSerializer extends JsonSerializer<BlindSignature> {
    @Override
    public void serialize(BlindSignature value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("amount", value.getAmount());
        gen.writeStringField("id", value.getKeySetId());
        gen.writeStringField("C_", value.getBlindedSignature().toString());
        gen.writeEndObject();
    }
}
