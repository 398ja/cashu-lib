package xyz.tcheeric.cashu.common.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import xyz.tcheeric.cashu.common.Keys;
import xyz.tcheeric.cashu.common.PublicKey;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

public class KeysSerializer extends JsonSerializer<Keys> {
    @Override
    public void serialize(Keys value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        for (Map.Entry<BigInteger, PublicKey> entry : value.getValues().entrySet()) {
            gen.writeStringField(String.valueOf(entry.getKey()), entry.getValue().toString());
        }
        gen.writeEndObject();
    }
}