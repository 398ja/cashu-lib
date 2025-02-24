package xyz.tcheeric.cashu.common.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.common.BlindSignature;
import xyz.tcheeric.cashu.common.codec.Decoder;

@AllArgsConstructor
public class BlindSignatudeDecoder implements Decoder<BlindSignature> {

    private final String jsonString;

    @Override
    public BlindSignature decode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, BlindSignature.class);
    }
}
