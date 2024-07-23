package cashu.common.json.codec.impl;

import cashu.common.json.codec.Decoder;
import cashu.common.util.Error;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ErrorDecoder implements Decoder<Error> {

    private final String jsonString;

    @Override
    public Error decode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, Error.class);
    }
}
