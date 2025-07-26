package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.P2PKSecret;
import xyz.tcheeric.cashu.common.WellKnownSecret;
import xyz.tcheeric.cashu.common.dto.WellKnownSecretDTO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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
                if (secret instanceof P2PKSecret) {
                    convertP2PKTagValues(tag);
                }
                secret.addTag(tag);
            }
        }
        return secret;
    }

    private void convertP2PKTagValues(WellKnownSecret.Tag tag) {
        switch (tag.getKey()) {
            case "sigflag" -> {
                List<Object> values = new ArrayList<>();
                for (Object v : tag.getValues()) {
                    if (v instanceof String s) {
                        values.add(P2PKSecret.SignatureFlag.valueOf(s));
                    } else {
                        values.add(v);
                    }
                }
                tag.setValues(values);
            }
            case "n_sigs", "locktime" -> {
                List<Object> values = new ArrayList<>();
                for (Object v : tag.getValues()) {
                    if (v instanceof Number n) {
                        values.add(n.intValue());
                    } else {
                        values.add(v);
                    }
                }
                tag.setValues(values);
            }
            case "pubkeys", "refund" -> {
                List<Object> values = new ArrayList<>();
                for (Object v : tag.getValues()) {
                    values.add(String.valueOf(v));
                }
                tag.setValues(values);
            }
        }
    }
}
