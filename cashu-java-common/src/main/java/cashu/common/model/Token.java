package cashu.common.model;

import cashu.common.json.serializer.TokenSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize(using = TokenSerializer.class)
public class Token {

    private final static String TOKEN_PREFIX = "cashu";
    private final static String URI_SCHEME = "cashu";
    private final static String TOKEN_VERSION = "A";


    @JsonProperty("token")
    @Builder.Default
    private final List<MintProof> mintProofs = new ArrayList<>();

    @JsonProperty
    @NonNull
    private String unit;

    @JsonProperty
    @Builder.Default
    private final String memo = "";

    public void addMintProof(@NonNull Token.MintProof mintProof) {
        if (this.mintProofs.contains(mintProof)) {
            return;
        }

        this.mintProofs.add(mintProof);
    }

    public static String serialize(@NonNull Token token) {
        return serialize(token, false);
    }

    public static String serialize(@NonNull Token token, boolean clickable) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonToken = objectMapper.writeValueAsString(token);
            if (clickable) {
                return URI_SCHEME + ":" + TOKEN_PREFIX + TOKEN_VERSION + Base64.getUrlEncoder().encodeToString(jsonToken.getBytes(StandardCharsets.UTF_8));
            }
            return TOKEN_PREFIX + TOKEN_VERSION + Base64.getUrlEncoder().encodeToString(jsonToken.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String serialize(String jsonToken) {
        return serialize(jsonToken, false);
    }

    public static String serialize(@NonNull String jsonToken, boolean clickable) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Token token = objectMapper.readValue(jsonToken, Token.class);
            return serialize(token, clickable);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Token deserialize(String serializedToken) {
        if (serializedToken.startsWith(URI_SCHEME + ":")) {
            serializedToken = serializedToken.substring(URI_SCHEME.length() + 1);
        }
        if (!serializedToken.startsWith(TOKEN_PREFIX + TOKEN_VERSION)) {
            throw new IllegalArgumentException("Invalid token format");
        }
        serializedToken = serializedToken.substring(TOKEN_PREFIX.length() + TOKEN_VERSION.length());

        String jsonToken = new String(Base64.getUrlDecoder().decode(serializedToken), StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(jsonToken, Token.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MintProof {

        @JsonProperty
        @NonNull
        private String mint;

        @JsonProperty
        @Builder.Default
        private final List<Proof> proofs = new ArrayList<>();

        public void addProof(@NonNull Proof proof) {
            if (this.proofs.contains(proof)) {
                return;
            }

            this.proofs.add(proof);
        }
    }
}
