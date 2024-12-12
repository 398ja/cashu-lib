package xyz.tcheeric.cashu.common.json.codec.impl;

import xyz.tcheeric.cashu.common.json.codec.Encoder;
import xyz.tcheeric.cashu.common.model.BlindSignature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BlindSignatureEncoder implements Encoder<BlindSignature> {

    private final BlindSignature blindSignature;

    @Override
    public String encode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(blindSignature);
    }
}
