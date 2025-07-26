package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenV4 implements Token {

    @JsonProperty("m")
    private String mintUrl;

    @JsonProperty("u")
    private String unit;

    @JsonProperty("d")
    private String memo;

    @JsonProperty("t")
    private Set<TokenData> tokenDataList = new HashSet<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TokenData {
        @JsonProperty("i")
        private byte[] keySetId;

        @JsonProperty("p")
        private Set<TokenProof> proofs = new HashSet<>();

        public void addProofs(@NonNull TokenProof... proofs) {
            Collections.addAll(this.proofs, proofs);
        }

        @Data
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class TokenProof {
            @JsonProperty("a")
            private Integer amount;

            @JsonProperty("s")
            private String secret;

            @JsonProperty("c")
            private byte[] signature;

            @JsonProperty("v")
            private DLEQProof dleqProof;

            @JsonProperty("w")
            private String witness;

            @Data
            @NoArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            private static class DLEQProof {
                @JsonProperty
                private String e;

                @JsonProperty
                private String s;

                @JsonProperty
                private String r;
            }
        }
    }

    @Override
    public String serialize(boolean clickable) {
        ObjectMapper objectMapper = JsonUtils.CBOR_MAPPER;
        try {
            byte[] cborToken = objectMapper.writeValueAsBytes(this);
            return TokenUtil.serialize(cborToken, Version.V4, clickable);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static TokenV4 deserialize(@NonNull String serializedToken) {
        if (!serializedToken.startsWith(TOKEN_PREFIX + Version.V4.getCode())) {
            throw new IllegalArgumentException("Invalid token format");
        }

        serializedToken = serializedToken.substring(TOKEN_PREFIX.length() + Version.V4.getCode().toString().length());

        byte[] cborToken = Base64.getUrlDecoder().decode(serializedToken);
        ObjectMapper objectMapper = JsonUtils.CBOR_MAPPER;
        try {
            return objectMapper.readValue(cborToken, TokenV4.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
