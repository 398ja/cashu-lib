package xyz.tcheeric.cashu.entities.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.entities.Proof;
import xyz.tcheeric.cashu.entities.codec.Encoder;

@AllArgsConstructor
public class ProofEncoder implements Encoder<Proof> {

    private final Proof proof;

    @Override
    public String encode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(proof);
    }
}
