package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
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
        CBORFactory factory = (CBORFactory) JsonUtils.CBOR_MAPPER.getFactory();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CBORGenerator gen = factory.createGenerator(out)) {
            log.debug("Serializing TokenV4 with {} token data entries", tokenDataList.size());

            int mapSize = 0;
            if (tokenDataList != null && !tokenDataList.isEmpty()) mapSize++;
            if (mintUrl != null) mapSize++;
            if (unit != null) mapSize++;
            if (memo != null) mapSize++;

            gen.writeStartObject(mapSize);

            if (tokenDataList != null && !tokenDataList.isEmpty()) {
                gen.writeFieldName("t");
                gen.writeStartArray(tokenDataList.size());
                for (TokenData td : tokenDataList) {
                    int tdSize = 0;
                    if (td.getKeySetId() != null) tdSize++;
                    if (td.getProofs() != null && !td.getProofs().isEmpty()) tdSize++;
                    gen.writeStartObject(tdSize);
                    if (td.getKeySetId() != null) {
                        gen.writeFieldName("i");
                        gen.writeBinary(td.getKeySetId());
                    }
                    if (td.getProofs() != null && !td.getProofs().isEmpty()) {
                        gen.writeFieldName("p");
                        gen.writeStartArray(td.getProofs().size());
                        for (TokenData.TokenProof proof : td.getProofs()) {
                            int proofSize = 0;
                            if (proof.getAmount() != null) proofSize++;
                            if (proof.getSecret() != null) proofSize++;
                            if (proof.getSignature() != null) proofSize++;
                            if (proof.getDleqProof() != null) proofSize++;
                            if (proof.getWitness() != null) proofSize++;
                            gen.writeStartObject(proofSize);
                            if (proof.getAmount() != null) {
                                gen.writeFieldName("a");
                                gen.writeNumber(proof.getAmount());
                            }
                            if (proof.getSecret() != null) {
                                gen.writeFieldName("s");
                                gen.writeString(proof.getSecret());
                            }
                            if (proof.getSignature() != null) {
                                gen.writeFieldName("c");
                                gen.writeBinary(proof.getSignature());
                            }
                            if (proof.getDleqProof() != null) {
                                TokenData.TokenProof.DLEQProof dp = proof.getDleqProof();
                                int dleqSize = 0;
                                if (dp.getE() != null) dleqSize++;
                                if (dp.getS() != null) dleqSize++;
                                if (dp.getR() != null) dleqSize++;
                                gen.writeFieldName("d");
                                gen.writeStartObject(dleqSize);
                                if (dp.getE() != null) {
                                    gen.writeFieldName("e");
                                    gen.writeBinary(dp.getE());
                                }
                                if (dp.getS() != null) {
                                    gen.writeFieldName("s");
                                    gen.writeBinary(dp.getS());
                                }
                                if (dp.getR() != null) {
                                    gen.writeFieldName("r");
                                    gen.writeBinary(dp.getR());
                                }
                                gen.writeEndObject();
                            }
                            if (proof.getWitness() != null) {
                                gen.writeFieldName("w");
                                gen.writeString(proof.getWitness());
                            }
                            gen.writeEndObject();
                        }
                        gen.writeEndArray();
                    }
                    gen.writeEndObject();
                }
                gen.writeEndArray();
            }

            if (mintUrl != null) {
                gen.writeFieldName("m");
                gen.writeString(mintUrl);
            }
            if (unit != null) {
                gen.writeFieldName("u");
                gen.writeString(unit);
            }
            if (memo != null) {
                gen.writeFieldName("d");
                gen.writeString(memo);
            }

            gen.writeEndObject();
            gen.flush();
            byte[] cborToken = out.toByteArray();
            return TokenUtil.serialize(cborToken, Version.V4, clickable);
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
