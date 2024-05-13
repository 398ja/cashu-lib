package cashu.common.json.codec.impl;

import cashu.common.json.codec.Decoder;
import cashu.common.model.Token;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

import java.util.logging.Level;

@AllArgsConstructor
@Log
public class TokenDecoder implements Decoder<Token> {

        private final String jsonString;

        @Override
        public Token decode() throws JsonProcessingException {
            ObjectMapper objectMapper = new ObjectMapper();
            log.log(Level.INFO, "Deserializing token: " + jsonString);
            return objectMapper.readValue(jsonString, Token.class);
        }
}
