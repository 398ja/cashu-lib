package xyz.tcheeric.cashu.common.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.common.KeySet;
import xyz.tcheeric.cashu.common.codec.Encoder;

@AllArgsConstructor
public class KeySetEncoder implements Encoder<KeySet> {

    private final KeySet keySet;

    @Override
    public String encode() throws JsonProcessingException {
        ObjectMapper objectMapper = JsonUtils.JSON_MAPPER;
        return objectMapper.writeValueAsString(keySet);
    }
}
