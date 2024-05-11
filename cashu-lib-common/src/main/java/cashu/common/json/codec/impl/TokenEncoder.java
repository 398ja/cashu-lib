package cashu.common.json.codec.impl;

import cashu.common.json.codec.Encoder;
import cashu.common.model.Token;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TokenEncoder implements Encoder<Token> {

    private final Token token;

    @Override
    public String encode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(token);
    }
}
