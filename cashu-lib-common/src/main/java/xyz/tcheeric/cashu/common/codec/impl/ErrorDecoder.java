package xyz.tcheeric.cashu.common.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.common.codec.Decoder;

@AllArgsConstructor
public class ErrorDecoder implements Decoder<Error> {

    private final String jsonString;

    @Override
    public Error decode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, Error.class);
    }
}
