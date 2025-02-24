package xyz.tcheeric.cashu.common.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;

import java.io.IOException;

public class ProofSerializer<T extends Secret> extends JsonSerializer<Proof<T>> {
    @Override
    public void serialize(Proof<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("amount", value.getAmount());
        gen.writeStringField("id", value.getKeySetId());
        gen.writeStringField("secret", value.getSecret().toString());
        gen.writeStringField("C", value.getUnblindedSignature().toString());
        gen.writeEndObject();
    }
}
