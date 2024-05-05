package cashu.common.json.codec.impl;

import cashu.common.model.Proof;
import cashu.common.json.codec.Decoder;
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
