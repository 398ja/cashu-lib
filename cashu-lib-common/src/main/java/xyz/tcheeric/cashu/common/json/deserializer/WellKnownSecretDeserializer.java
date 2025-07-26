package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.P2PKSecret;
import xyz.tcheeric.cashu.common.WellKnownSecret;
import xyz.tcheeric.cashu.common.dto.WellKnownSecretDTO;

import java.io.IOException;

@Log
public class WellKnownSecretDeserializer extends JsonDeserializer<WellKnownSecret> {
    @Override
    public WellKnownSecret deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        WellKnownSecretDTO dto = mapper.readValue(p, WellKnownSecretDTO.class);

        WellKnownSecret secret = switch (dto.getKind()) {
            case P2PK -> new P2PKSecret();
            default -> throw new IllegalArgumentException("Invalid kind");
        };

        secret.setNonce(dto.getNonce());
        if (dto.getData() != null) {
            secret.setData(Hex.decode(dto.getData()));
        }
        if (dto.getTags() != null) {
            for (WellKnownSecret.Tag tag : dto.getTags()) {
                secret.addTag(tag);
            }
        }
        return secret;
    }
}
