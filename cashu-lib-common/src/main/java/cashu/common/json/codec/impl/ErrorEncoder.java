package cashu.common.json.codec.impl;

import cashu.common.json.codec.Encoder;
import cashu.common.util.Error;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ErrorEncoder implements Encoder<Error> {

    private final Error error;

    @Override
    public String encode() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(error);
    }
}
