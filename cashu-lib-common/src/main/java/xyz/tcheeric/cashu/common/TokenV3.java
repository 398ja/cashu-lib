package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenV3<T extends Secret> implements Token {

    @JsonProperty("token")
    private Set<MintProof<T>> mintProofs = new HashSet<>();

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("memo")
    private String memo;

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MintProof<T extends Secret> {
        @JsonProperty("mint")
        private String mint;

        @JsonProperty("proofs")
        private Set<Proof<T>> proofs = new HashSet<>();

        public boolean addProof(@NonNull Proof<T> proof) {
            return this.proofs.add(proof);
        }

        public boolean removeProof(@NonNull Proof<T> proof) {
            return this.proofs.remove(proof);
        }
    }

    @Override
    public String serialize(boolean clickable) {
        ObjectMapper objectMapper = JsonUtils.JSON_MAPPER;
        try {
            byte[] json = objectMapper.writeValueAsBytes(this);
            return TokenUtil.serialize(json, Version.V3, clickable);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO - Test me!!!
    public void addMintProof(@NonNull MintProof<T> mintProof) {
        MintProof existingMintProof = this.mintProofs.stream()
                .filter(mp -> mp.getMint().equals(mintProof.getMint()))
                .findFirst()
                .orElse(null);
        if (existingMintProof == null) {
            this.mintProofs.add(mintProof);
        } else {
            existingMintProof.getProofs().addAll(mintProof.getProofs());
        }
    }

    public static TokenV3 deserialize(String serializedToken) {
        if (!serializedToken.startsWith(TOKEN_PREFIX + Version.V3.getCode())) {
            throw new IllegalArgumentException("Invalid token format");
        }

        serializedToken = serializedToken.substring(TOKEN_PREFIX.length() + Version.V3.getCode().toString().length());

        byte[] byteArrToken = Base64.getUrlDecoder().decode(serializedToken);
        ObjectMapper objectMapper = JsonUtils.JSON_MAPPER;
        try {
            return objectMapper.readValue(byteArrToken, TokenV3.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}