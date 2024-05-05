package cashu.common.json.codec.impl;

import cashu.common.json.codec.Encoder;
import cashu.common.model.KeySet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KeySetEncoder implements Encoder<KeySet> {

    private final KeySet keySet;

    @Override
    public String encode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(keySet);
    }
}
