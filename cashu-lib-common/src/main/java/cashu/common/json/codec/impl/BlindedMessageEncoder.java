package cashu.common.json.codec.impl;

import cashu.common.json.codec.Encoder;
import cashu.common.model.BlindedMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BlindedMessageEncoder implements Encoder<BlindedMessage> {

    private final BlindedMessage blindedMessage;

    @Override
    public String encode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(blindedMessage);
    }
}
