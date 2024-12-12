package xyz.tcheeric.cashu.common.json.codec.impl;

import xyz.tcheeric.cashu.common.json.codec.Decoder;
import xyz.tcheeric.cashu.common.model.BlindedMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BlindedMessageDecoder implements Decoder<BlindedMessage> {

    private final String jsonString;

    @Override
    public BlindedMessage decode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, BlindedMessage.class);
    }
}
