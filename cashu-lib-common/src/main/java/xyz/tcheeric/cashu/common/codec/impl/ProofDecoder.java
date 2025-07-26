package xyz.tcheeric.cashu.common.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.codec.Decoder;

@AllArgsConstructor
public class ProofDecoder<T extends Secret> implements Decoder<Proof<T>> {

    private final String jsonString;

    @Override
    public Proof<T> decode() throws JsonProcessingException {
        ObjectMapper objectMapper = JsonUtils.JSON_MAPPER;
        return objectMapper.readValue(jsonString, Proof.class);
    }
}
