package xyz.tcheeric.cashu.common.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.WellKnownSecret;
import xyz.tcheeric.cashu.common.dto.WellKnownSecretDTO;

import java.io.IOException;

public class WellKnownSecretSerializer extends JsonSerializer<WellKnownSecret> {

    @Override
    public void serialize(WellKnownSecret value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        ObjectMapper mapper = (ObjectMapper) gen.getCodec();
        WellKnownSecretDTO dto = new WellKnownSecretDTO();
        dto.setKind(value.getKind());
        dto.setNonce(value.getNonce());
        dto.setData(Hex.toHexString(value.getData()));
        dto.setTags(value.getTags());
        mapper.writeValue(gen, dto);
    }
}
