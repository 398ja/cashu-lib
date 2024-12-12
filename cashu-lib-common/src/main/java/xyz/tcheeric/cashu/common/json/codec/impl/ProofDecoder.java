package xyz.tcheeric.cashu.common.json.codec.impl;

import xyz.tcheeric.cashu.common.json.codec.Decoder;
import xyz.tcheeric.cashu.common.model.Proof;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ProofDecoder implements Decoder<Proof> {

    private final String jsonString;

    @Override
    public Proof decode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, Proof.class);
    }
}
