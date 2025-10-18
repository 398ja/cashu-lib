package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"t", "d", "m", "u"})
public class TokenV4 implements Token {

    @JsonProperty("m")
    private String mintUrl;

    @JsonProperty("u")
    private String unit;

    @JsonProperty("d")
    private String memo;

    @JsonProperty("t")
    private List<TokenData> tokenDataList = new ArrayList<>();

    public void setMintUrl(String mintUrl) {
        if (mintUrl != null) {
            this.mintUrl = mintUrl.replaceAll("/+$", "");
        } else {
            this.mintUrl = null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"i", "p"})
    public static class TokenData {
        @JsonProperty("i")
        private byte[] keySetId;

        @JsonProperty("p")
        private List<TokenProof> proofs = new ArrayList<>();

        public void addProofs(@NonNull TokenProof... proofs) {
            Collections.addAll(this.proofs, proofs);
        }

        @Data
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonPropertyOrder({"a", "s", "c", "d", "w"})
        public static class TokenProof {
            @JsonProperty("a")
            private Integer amount;

            @JsonProperty("s")
            private String secret;

            @JsonProperty("c")
            private byte[] signature;

            @JsonProperty("d")
            private DLEQProof dleqProof;

            @JsonProperty("w")
            private String witness;

            @Data
            @NoArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonPropertyOrder({"e", "s", "r"})
            public static class DLEQProof {
                @JsonProperty("e")
                private byte[] e;

                @JsonProperty("s")
                private byte[] s;

                @JsonProperty("r")
                private byte[] r;
            }
        }
    }

    @Override
    public String serialize(boolean clickable) {
        try {
            log.debug("Serializing TokenV4 with {} token data entries", tokenDataList.size());

            // Use Jackson's built-in CBOR serialization instead of manual generation
            // This ensures compatibility across Jackson versions and proper deserialization
            byte[] cborToken = JsonUtils.CBOR_MAPPER.writeValueAsBytes(this);
            return TokenUtil.serialize(cborToken, Version.V4, clickable);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TokenV4 deserialize(@NonNull String serializedToken) {
        // Accept clickable URI format (e.g., "cashu:cashuB...") by stripping the scheme if present
        if (serializedToken.startsWith(URI_SCHEME)) {
            serializedToken = serializedToken.substring(URI_SCHEME.length());
        }

        if (!serializedToken.startsWith(TOKEN_PREFIX + Version.V4.getCode())) {
            throw new IllegalArgumentException("Invalid token format");
        }

        serializedToken = serializedToken.substring(TOKEN_PREFIX.length() + Version.V4.getCode().toString().length());

        byte[] cborToken = Base64.getUrlDecoder().decode(serializedToken);
        ObjectMapper objectMapper = JsonUtils.CBOR_MAPPER;
        try {
            TokenV4 token = objectMapper.readValue(cborToken, TokenV4.class);
            log.debug("Deserialized TokenV4 with {} token data entries", token.getTokenDataList().size());
            return token;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
