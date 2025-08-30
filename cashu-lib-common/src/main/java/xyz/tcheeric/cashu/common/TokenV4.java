package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
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
    private Set<TokenData> tokenDataList = new HashSet<>();

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
        private Set<TokenProof> proofs = new HashSet<>();

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

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CBORFactory factory = (CBORFactory) JsonUtils.CBOR_MAPPER.getFactory();
            try (CBORGenerator gen = factory.createGenerator(out)) {
                int topLevelFields = 1; // t is always present
                if (memo != null) topLevelFields++;
                if (mintUrl != null) topLevelFields++;
                if (unit != null) topLevelFields++;

                gen.writeStartObject(topLevelFields);

                // t: token data
                gen.writeFieldName("t");
                gen.writeStartArray(tokenDataList.size());
                for (TokenData td : tokenDataList) {
                    gen.writeStartObject(2);
                    gen.writeFieldName("i");
                    gen.writeBinary(td.getKeySetId());
                    gen.writeFieldName("p");
                    gen.writeStartArray(td.getProofs().size());
                    for (TokenData.TokenProof proof : td.getProofs()) {
                        int proofFields = 3; // a, s, c are required
                        if (proof.getDleqProof() != null) proofFields++;
                        if (proof.getWitness() != null) proofFields++;
                        gen.writeStartObject(proofFields);
                        gen.writeFieldName("a");
                        gen.writeNumber(proof.getAmount());
                        gen.writeFieldName("s");
                        gen.writeString(proof.getSecret());
                        gen.writeFieldName("c");
                        gen.writeBinary(proof.getSignature());
                        if (proof.getDleqProof() != null) {
                            gen.writeFieldName("d");
                            TokenData.TokenProof.DLEQProof dleq = proof.getDleqProof();
                            gen.writeStartObject(3);
                            gen.writeFieldName("e");
                            gen.writeBinary(dleq.getE());
                            gen.writeFieldName("s");
                            gen.writeBinary(dleq.getS());
                            gen.writeFieldName("r");
                            gen.writeBinary(dleq.getR());
                            gen.writeEndObject();
                        }
                        if (proof.getWitness() != null) {
                            gen.writeFieldName("w");
                            gen.writeString(proof.getWitness());
                        }
                        gen.writeEndObject();
                    }
                    gen.writeEndArray();
                    gen.writeEndObject();
                }
                gen.writeEndArray();

                if (memo != null) {
                    gen.writeStringField("d", memo);
                }
                if (mintUrl != null) {
                    gen.writeStringField("m", mintUrl);
                }
                if (unit != null) {
                    gen.writeStringField("u", unit);
                }

                gen.writeEndObject();
            }

            return TokenUtil.serialize(out.toByteArray(), Version.V4, clickable);
        } catch (IOException e) {
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
            TokenV4 token = objectMapper.readValue(cborToken, TokenV4.class);
            log.debug("Deserialized TokenV4 with {} token data entries", token.getTokenDataList().size());
            return token;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
