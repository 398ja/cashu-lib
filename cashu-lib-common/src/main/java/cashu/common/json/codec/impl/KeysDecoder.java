package cashu.common.json.codec.impl;

import cashu.common.json.codec.Decoder;
import cashu.common.model.Keys;
import cashu.common.model.PublicKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

import java.math.BigInteger;
import java.util.Map;

@AllArgsConstructor
public class KeysDecoder implements Decoder<Keys> {

    private final String jsonString;

    @Override
    public Keys decode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, PublicKey> map = objectMapper.readValue(jsonString, new TypeReference<>() {});
        Keys keys = new Keys();
        for (Map.Entry<String, PublicKey> entry : map.entrySet()) {
            keys.put(new BigInteger(entry.getKey()), entry.getValue());
        }
        return keys;
    }
}
