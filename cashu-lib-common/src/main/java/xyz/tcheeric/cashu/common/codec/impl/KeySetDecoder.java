package xyz.tcheeric.cashu.common.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.common.KeySet;
import xyz.tcheeric.cashu.common.codec.Decoder;

@AllArgsConstructor
public class KeySetDecoder implements Decoder<KeySet> {

    private final String jsonString;

    @Override
    public KeySet decode() throws JsonProcessingException {
        ObjectMapper objectMapper = JsonUtils.JSON_MAPPER;
        return objectMapper.readValue(jsonString, KeySet.class);
    }
}
