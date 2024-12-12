package xyz.tcheeric.cashu.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenV3 implements Token {

    @JsonProperty("token")
    private Set<MintProof> mintProofs = new HashSet<>();

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("memo")
    private String memo;

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MintProof {
        @JsonProperty("mint")
        private String mint;

        @JsonProperty("proofs")
        private Set<Proof> proofs = new HashSet<>();
    }

    @Override
    public String serialize(boolean clickable) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            byte[] json = objectMapper.writeValueAsBytes(this);
            return TokenUtil.serialize(json, Version.V3, clickable);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static TokenV3 deserialize(String serializedToken) {
        if (!serializedToken.startsWith(TOKEN_PREFIX + Version.V3.getCode())) {
            throw new IllegalArgumentException("Invalid token format");
        }

        serializedToken = serializedToken.substring(TOKEN_PREFIX.length() + Version.V3.getCode().toString().length());

        byte[] byteArrToken = Base64.getUrlDecoder().decode(serializedToken);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(byteArrToken, TokenV3.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}