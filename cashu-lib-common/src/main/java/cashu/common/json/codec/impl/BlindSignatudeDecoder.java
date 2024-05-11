package cashu.common.json.codec.impl;

import cashu.common.json.codec.Decoder;
import cashu.common.model.BlindSignature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BlindSignatudeDecoder implements Decoder<BlindSignature> {

    private final String jsonString;

    @Override
    public BlindSignature decode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, BlindSignature.class);
    }
}
