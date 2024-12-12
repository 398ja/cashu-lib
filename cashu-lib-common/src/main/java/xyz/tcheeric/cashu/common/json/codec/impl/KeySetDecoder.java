package xyz.tcheeric.cashu.common.json.codec.impl;

import xyz.tcheeric.cashu.common.json.codec.Decoder;
import xyz.tcheeric.cashu.common.model.KeySet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KeySetDecoder implements Decoder<KeySet> {

    private final String jsonString;

    @Override
    public KeySet decode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, KeySet.class);
    }
}
