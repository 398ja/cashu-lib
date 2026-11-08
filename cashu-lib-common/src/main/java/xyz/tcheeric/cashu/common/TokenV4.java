package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import xyz.tcheeric.cashu.common.voucher.VoucherDocument;
import xyz.tcheeric.cashu.common.voucher.VoucherDocuments;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

@Data
@NoArgsConstructor
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"t", "d", "m", "u", "v"})
public class TokenV4 implements Token {

    @JsonProperty("m")
    private String mintUrl;

    @JsonProperty("u")
    private String unit;

    @JsonProperty("d")
    private String memo;

    @JsonProperty("t")
    private List<TokenData> tokenDataList = new ArrayList<>();

    /**
     * Voucher documents carried by reference (cashu-voucher#64): {@code hex(SHA256(D)) -> D}.
     * Written last and only when non-empty, so tokens without documents encode exactly as before.
     */
    @JsonProperty("v")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Map<String, VoucherDocument> voucherDocumentMap = new TreeMap<>();

    /** Returns the carried voucher documents, sorted by key, as defensive copies. */
    @JsonIgnore
    public Map<String, byte[]> getVoucherDocuments() {
        Map<String, byte[]> copy = new LinkedHashMap<>();
        documents().forEach((key, document) -> copy.put(key, document.toBytes()));
        return Collections.unmodifiableMap(copy);
    }

    /** Whether this token carries at least one voucher document. */
    @JsonIgnore
    public boolean hasVoucherDocuments() {
        return !documents().isEmpty();
    }

    /**
     * Stores a voucher document and returns its key, {@code hex(SHA256(document))}.
     *
     * @throws IllegalArgumentException when the document is empty or larger than
     *                                  {@link VoucherDocuments#MAX_DOCUMENT_BYTES}
     */
    public String putVoucherDocument(@NonNull byte[] document) {
        VoucherDocument value = VoucherDocument.of(document);
        String key = value.key();
        VoucherDocuments.validate(Map.of(key, document), 1);
        documents().put(key, value);
        return key;
    }

    /** Drops every document whose key the predicate does not accept (for example, unreferenced ones). */
    public void retainVoucherDocuments(@NonNull Predicate<String> referenced) {
        documents().keySet().removeIf(referenced.negate());
    }

    /** Drops every document whose key is not in the given set of referenced keys. */
    public void retainReferencedDocuments(@NonNull Set<String> referencedKeys) {
        retainVoucherDocuments(referencedKeys::contains);
    }

    private Map<String, VoucherDocument> documents() {
        if (voucherDocumentMap == null) {
            voucherDocumentMap = new TreeMap<>();
        } else if (!(voucherDocumentMap instanceof TreeMap)) {
            voucherDocumentMap = new TreeMap<>(voucherDocumentMap);
        }
        return voucherDocumentMap;
    }

    /** Total number of proofs across every keyset entry. */
    @JsonIgnore
    public int getProofCount() {
        if (tokenDataList == null) {
            return 0;
        }
        return tokenDataList.stream()
                .filter(Objects::nonNull)
                .mapToInt(td -> td.getProofs() == null ? 0 : td.getProofs().size())
                .sum();
    }

    /**
     * Checks the carried documents against the container rules.
     *
     * @throws IllegalArgumentException when a key is malformed or does not hash its value, a value is
     *                                  empty or too large, or there are more documents than proofs
     */
    public void validateVoucherDocuments() {
        Map<String, byte[]> raw = new LinkedHashMap<>();
        documents().forEach((key, document) -> raw.put(key, document == null ? null : document.toBytes()));
        VoucherDocuments.validate(raw, getProofCount());
    }

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
        log.debug("Serializing TokenV4 with {} token data entries", tokenDataList.size());

        // Use the definite-length CBOR encoder so cashu-ts (and other definite-length-only
        // decoders) can parse tokens we produce. Jackson's CBOR module emits indefinite-length
        // maps/arrays by default, which cashu-ts rejects.
        byte[] cborToken = TokenV4CborEncoder.encode(this);
        return TokenUtil.serialize(cborToken, Version.V4, clickable);
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
            token.validateVoucherDocuments();
            log.debug("Deserialized TokenV4 with {} token data entries", token.getTokenDataList().size());
            return token;
        } catch (IOException e) {
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof IllegalArgumentException invalid) {
                    throw invalid;
                }
            }
            throw new RuntimeException(e);
        }
    }
}
