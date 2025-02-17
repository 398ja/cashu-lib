package xyz.tcheeric.cashu.entities.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.entities.KeySet;
import xyz.tcheeric.cashu.entities.codec.Encoder;

@AllArgsConstructor
public class KeySetEncoder implements Encoder<KeySet> {

    private final KeySet keySet;

    @Override
    public String encode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(keySet);
    }
}
