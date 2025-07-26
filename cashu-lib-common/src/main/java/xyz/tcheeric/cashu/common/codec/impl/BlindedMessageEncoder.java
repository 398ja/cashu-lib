package xyz.tcheeric.cashu.common.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.codec.Encoder;

@AllArgsConstructor
public class BlindedMessageEncoder implements Encoder<BlindedMessage> {

    private final BlindedMessage blindedMessage;

    @Override
    public String encode() throws JsonProcessingException {
        ObjectMapper objectMapper = JsonUtils.JSON_MAPPER;
        return objectMapper.writeValueAsString(blindedMessage);
    }
}
