package xyz.tcheeric.cashu.common.codec.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import xyz.tcheeric.cashu.common.codec.Encoder;

@AllArgsConstructor
public class ErrorEncoder implements Encoder<Error> {

    private final Error error;

    @Override
    public String encode() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(error);
    }
}
