package xyz.tcheeric.cashu.common.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.codec.Decoder;

@AllArgsConstructor
public class BlindedMessageDecoder implements Decoder<BlindedMessage> {

    private final String jsonString;

    @Override
    public BlindedMessage decode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, BlindedMessage.class);
    }
}
