package xyz.tcheeric.cashu.common.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.common.BlindSignature;
import xyz.tcheeric.cashu.common.codec.Encoder;

@AllArgsConstructor
public class BlindSignatureEncoder implements Encoder<BlindSignature> {

    private final BlindSignature blindSignature;

    @Override
    public String encode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(blindSignature);
    }
}
