package xyz.tcheeric.cashu.entities.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.entities.Proof;
import xyz.tcheeric.cashu.entities.codec.Decoder;

@AllArgsConstructor
public class ProofDecoder implements Decoder<Proof> {

    private final String jsonString;

    @Override
    public Proof decode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, Proof.class);
    }
}
