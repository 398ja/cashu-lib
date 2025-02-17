package xyz.tcheeric.cashu.entities.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import xyz.tcheeric.cashu.entities.Proof;

import java.io.IOException;

public class ProofSerializer extends JsonSerializer<Proof> {
    @Override
    public void serialize(Proof value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("amount", value.getAmount());
        gen.writeStringField("id", value.getKeySetId());
        gen.writeStringField("secret", value.getSecret().toString());
        gen.writeStringField("C", value.getUnblindedSignature().toString());
        gen.writeEndObject();
    }
}
