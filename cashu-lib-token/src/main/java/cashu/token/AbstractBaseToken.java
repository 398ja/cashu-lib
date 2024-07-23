package cashu.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class AbstractBaseToken implements Token {

    private Version version;
    private String prefix;
    private boolean clickable;

    @Override
    public String serialize() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonToken = objectMapper.writeValueAsString(this);
            return AbstractBaseToken.serialize(jsonToken, prefix, version, clickable);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String serialize(@NonNull String jsonToken, @NonNull String prefix, @NonNull Version version, boolean clickable) {
        if (clickable) {
            return URI_SCHEME + prefix + version.getCode() + Base64.getUrlEncoder().encodeToString(jsonToken.getBytes(StandardCharsets.UTF_8));
        }
        return prefix + version.getCode() + Base64.getUrlEncoder().encodeToString(jsonToken.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Token deserialize(@NonNull String serializedToken) {
        if (serializedToken.startsWith(URI_SCHEME)) {
            serializedToken = serializedToken.substring(URI_SCHEME.length());
        }
        if (!serializedToken.startsWith(prefix + version.getCode())) {
            throw new IllegalArgumentException("Invalid token format");
        }
        serializedToken = serializedToken.substring(prefix.length() + version.getCode().toString().length());

        String jsonToken = new String(Base64.getUrlDecoder().decode(serializedToken), StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(jsonToken, Token.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }



}
