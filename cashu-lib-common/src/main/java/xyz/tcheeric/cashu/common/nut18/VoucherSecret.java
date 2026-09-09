package xyz.tcheeric.cashu.common.nut18;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * NUT-10 compliant voucher secret using tag-based storage.
 *
 * <p>The voucher ID is stored in the data field as a UUID string, and all
 * voucher metadata (issuer, unit, face value, etc.) is stored as NUT-10 tags.
 *
 * <p>Serialization format:
 * <pre>
 * ["VOUCHER", "voucherId_hex", "nonce", [
 *     ["issuer", "merchant123"],
 *     ["unit", "sat"],
 *     ["face_value", "5000"],
 *     ["expires_at", "1736380800"],
 *     ["memo", "Gift card"],
 *     ["face_decimals", "2"],
 *     ["issuer_sig", "5f3a8b2c..."],
 *     ["issuer_pubkey", "02abc123..."]
 * ]]
 * </pre>
 */
@Slf4j
public class VoucherSecret extends WellKnownSecret {

    public VoucherSecret() {
        super(Kind.VOUCHER);
    }

    /**
     * Creates a voucher secret with the given voucher ID.
     * A unique nonce is automatically generated.
     *
     * @param voucherId the unique voucher identifier
     */
    public VoucherSecret(@NonNull UUID voucherId) {
        super(Kind.VOUCHER, voucherId.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates a voucher secret with explicit nonce.
     * Use this when you need deterministic secrets (e.g., for testing).
     *
     * @param voucherId the unique voucher identifier
     * @param nonce unique nonce for this proof
     */
    public VoucherSecret(@NonNull UUID voucherId, @NonNull String nonce) {
        super(Kind.VOUCHER);
        this.setData(voucherId.toString().getBytes(StandardCharsets.UTF_8));
        this.setNonce(nonce);
    }

    // ===== Voucher ID (from data field) =====

    /**
     * Gets the voucher ID.
     *
     * @return the voucher UUID, or null if not set or data is not a valid UUID
     */
    public UUID getVoucherId() {
        byte[] data = getData();
        if (data == null) {
            return null;
        }
        try {
            return UUID.fromString(new String(data, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            log.warn("voucher_secret get_voucher_id invalid_uuid data_length={}", data.length);
            return null;
        }
    }

    /**
     * Sets the voucher ID.
     *
     * @param voucherId the voucher UUID
     */
    public void setVoucherId(@NonNull UUID voucherId) {
        setData(voucherId.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ===== Tag-based getters =====

    /**
     * Gets the issuer/merchant identifier.
     *
     * @return the issuer ID, or null if not set
     */
    public String getIssuerId() {
        return getTagValue(VoucherTags.ISSUER);
    }

    /**
     * Gets the currency unit.
     *
     * @return the unit (e.g., "sat", "usd"), or null if not set
     */
    public String getUnit() {
        return getTagValue(VoucherTags.UNIT);
    }

    /**
     * Gets the face value in the smallest unit.
     *
     * @return the face value, or null if not set or invalid
     */
    public Long getFaceValue() {
        String val = getTagValue(VoucherTags.FACE_VALUE);
        if (val == null) {
            return null;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            log.warn("voucher_secret get_face_value invalid_format value={}", val);
            return null;
        }
    }

    /**
     * Gets the expiry timestamp.
     *
     * @return the Unix epoch seconds, or null if not set or invalid
     */
    public Long getExpiresAt() {
        String val = getTagValue(VoucherTags.EXPIRES_AT);
        if (val == null) {
            return null;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            log.warn("voucher_secret get_expires_at invalid_format value={}", val);
            return null;
        }
    }

    /**
     * Gets the human-readable memo.
     *
     * @return the memo, or null if not set
     */
    public String getMemo() {
        return getTagValue(VoucherTags.MEMO);
    }

    /**
     * Gets the face value decimal places.
     *
     * @return the decimals, or 0 if not set or invalid
     */
    public int getFaceDecimals() {
        String val = getTagValue(VoucherTags.FACE_DECIMALS);
        if (val == null) {
            return 0;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("voucher_secret get_face_decimals invalid_format value={}", val);
            return 0;
        }
    }

    /**
     * Gets the backing strategy.
     *
     * @return the strategy (FIXED, MINIMAL, PROPORTIONAL), or "FIXED" if not set
     */
    public String getBackingStrategy() {
        String val = getTagValue(VoucherTags.BACKING_STRATEGY);
        return val != null ? val : "FIXED";
    }

    /**
     * Gets the issuance ratio.
     *
     * @return the ratio, or 1.0 if not set or invalid
     */
    public double getIssuanceRatio() {
        String val = getTagValue(VoucherTags.ISSUANCE_RATIO);
        if (val == null) {
            return 1.0;
        }
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            log.warn("voucher_secret get_issuance_ratio invalid_format value={}", val);
            return 1.0;
        }
    }

    /**
     * Gets the issuer's Schnorr signature.
     *
     * @return the hex-encoded signature, or null if not set
     */
    public String getIssuerSignature() {
        return getTagValue(VoucherTags.ISSUER_SIG);
    }

    /**
     * Gets the issuer's public key.
     *
     * @return the hex-encoded public key, or null if not set
     */
    public String getIssuerPublicKey() {
        return getTagValue(VoucherTags.ISSUER_PUBKEY);
    }

    /**
     * Gets the merchant metadata JSON.
     *
     * @return the JSON string, or null if not set
     */
    public String getMerchantMetadata() {
        return getTagValue(VoucherTags.MERCHANT_METADATA);
    }

    // ===== Tag-based setters =====

    /**
     * Sets the issuer/merchant identifier.
     *
     * @param issuerId the issuer ID
     */
    public void setIssuerId(@NonNull String issuerId) {
        setTag(VoucherTags.ISSUER, List.of(issuerId));
    }

    /**
     * Sets the currency unit.
     *
     * @param unit the unit (e.g., "sat", "usd")
     */
    public void setUnit(@NonNull String unit) {
        setTag(VoucherTags.UNIT, List.of(unit));
    }

    /**
     * Sets the face value.
     *
     * @param faceValue the face value in smallest unit
     */
    public void setFaceValue(long faceValue) {
        setTag(VoucherTags.FACE_VALUE, List.of(faceValue));
    }

    /**
     * Sets the expiry timestamp.
     *
     * @param expiresAt Unix epoch seconds, or null to clear
     */
    public void setExpiresAt(Long expiresAt) {
        if (expiresAt != null) {
            setTag(VoucherTags.EXPIRES_AT, List.of(expiresAt));
        } else {
            Tag tag = getTag(VoucherTags.EXPIRES_AT);
            if (tag != null) {
                removeTag(tag);
            }
        }
    }

    /**
     * Sets the human-readable memo.
     *
     * @param memo the memo, or null to clear
     */
    public void setMemo(String memo) {
        if (memo != null && !memo.isBlank()) {
            setTag(VoucherTags.MEMO, List.of(memo));
        } else {
            Tag tag = getTag(VoucherTags.MEMO);
            if (tag != null) {
                removeTag(tag);
            }
        }
    }

    /**
     * Sets the face value decimal places.
     *
     * @param faceDecimals number of decimal places
     */
    public void setFaceDecimals(int faceDecimals) {
        if (faceDecimals > 0) {
            setTag(VoucherTags.FACE_DECIMALS, List.of(faceDecimals));
        } else {
            Tag tag = getTag(VoucherTags.FACE_DECIMALS);
            if (tag != null) {
                removeTag(tag);
            }
        }
    }

    /**
     * Sets the backing strategy.
     *
     * @param backingStrategy the strategy (FIXED, MINIMAL, PROPORTIONAL), or null to use default
     */
    public void setBackingStrategy(String backingStrategy) {
        if (backingStrategy != null && !backingStrategy.isBlank()) {
            setTag(VoucherTags.BACKING_STRATEGY, List.of(backingStrategy));
        } else {
            Tag tag = getTag(VoucherTags.BACKING_STRATEGY);
            if (tag != null) {
                removeTag(tag);
            }
        }
    }

    /**
     * Sets the issuance ratio.
     *
     * @param issuanceRatio the ratio, or null to use default (1.0)
     */
    public void setIssuanceRatio(Double issuanceRatio) {
        if (issuanceRatio != null && issuanceRatio != 1.0) {
            setTag(VoucherTags.ISSUANCE_RATIO, List.of(issuanceRatio));
        } else {
            Tag tag = getTag(VoucherTags.ISSUANCE_RATIO);
            if (tag != null) {
                removeTag(tag);
            }
        }
    }

    /**
     * Sets the issuer's Schnorr signature.
     *
     * @param signature hex-encoded signature
     */
    public void setIssuerSignature(@NonNull String signature) {
        setTag(VoucherTags.ISSUER_SIG, List.of(signature));
    }

    /**
     * Sets the issuer's public key.
     *
     * @param publicKey hex-encoded public key
     */
    public void setIssuerPublicKey(@NonNull String publicKey) {
        setTag(VoucherTags.ISSUER_PUBKEY, List.of(publicKey));
    }

    /**
     * Sets the merchant metadata.
     *
     * @param metadata JSON string, or null to clear
     */
    public void setMerchantMetadata(String metadata) {
        if (metadata != null && !metadata.isBlank()) {
            setTag(VoucherTags.MERCHANT_METADATA, List.of(metadata));
        } else {
            Tag tag = getTag(VoucherTags.MERCHANT_METADATA);
            if (tag != null) {
                removeTag(tag);
            }
        }
    }

    // ===== Helper methods =====

    /**
     * Checks if this voucher is expired.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        Long expiresAt = getExpiresAt();
        if (expiresAt == null) {
            return false;
        }
        return System.currentTimeMillis() / 1000 > expiresAt;
    }

    /**
     * Whether the signature and public key tags are both present.
     *
     * <p>Named for what it checks. The former name, {@code isSigned()}, read as "this voucher's
     * signature is good" and meant only "the fields are filled in" (audit L-11), which is a
     * dangerous thing for a method guarding a redemption path to be ambiguous about: an attacker
     * supplies both fields trivially. Verifying the signature is
     * {@code VoucherSignatureService.verify}, and even that only proves the voucher is
     * self-consistent unless the key is checked against a registered issuer.
     *
     * @return true if both the signature and public key tags are present
     */
    public boolean hasSignatureFields() {
        return getIssuerSignature() != null && getIssuerPublicKey() != null;
    }

    /**
     * @return true if both the signature and public key tags are present
     * @deprecated Misleading name: this never verified anything. Use {@link #hasSignatureFields()}.
     */
    @Deprecated(forRemoval = true)
    public boolean isSigned() {
        return hasSignatureFields();
    }

    /**
     * Gets a tag value as a string.
     *
     * @param key the tag key
     * @return the first value as string, or null if not found
     */
    private String getTagValue(String key) {
        Tag tag = getTag(key);
        if (tag != null && !tag.getValues().isEmpty()) {
            return tag.getValues().get(0).toString();
        }
        return null;
    }

    // ===== Builder =====

    /**
     * Creates a new builder for VoucherSecret.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for VoucherSecret with fluent API.
     *
     * <p><b>Field Requirements:</b>
     * <ul>
     *   <li>{@code voucherId} - Optional. Auto-generated UUID if not provided.</li>
     *   <li>{@code nonce} - Optional. Auto-generated if not provided.</li>
     *   <li>{@code issuerId} - Recommended for identifying the voucher issuer.</li>
     *   <li>{@code unit} - Recommended for specifying the currency unit.</li>
     *   <li>{@code faceValue} - Recommended for specifying the voucher value.</li>
     *   <li>{@code expiresAt} - Optional. Unix timestamp for expiration.</li>
     *   <li>{@code memo} - Optional. Human-readable description.</li>
     *   <li>{@code faceDecimals} - Optional. Defaults to 0.</li>
     *   <li>{@code backingStrategy} - Optional. Defaults to "FIXED".</li>
     *   <li>{@code issuanceRatio} - Optional. Defaults to 1.0.</li>
     *   <li>{@code issuerSignature} - Optional. Required for signed vouchers.</li>
     *   <li>{@code issuerPublicKey} - Optional. Required for signed vouchers.</li>
     *   <li>{@code merchantMetadata} - Optional. Custom JSON metadata.</li>
     * </ul>
     */
    public static class Builder {
        private UUID voucherId;
        private String nonce;
        private String issuerId;
        private String unit;
        private Long faceValue;
        private Long expiresAt;
        private String memo;
        private Integer faceDecimals;
        private String backingStrategy;
        private Double issuanceRatio;
        private String issuerSignature;
        private String issuerPublicKey;
        private String merchantMetadata;

        public Builder voucherId(UUID id) {
            this.voucherId = id;
            return this;
        }

        public Builder nonce(String nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder issuerId(String id) {
            this.issuerId = id;
            return this;
        }

        public Builder unit(String u) {
            this.unit = u;
            return this;
        }

        public Builder faceValue(long v) {
            this.faceValue = v;
            return this;
        }

        public Builder expiresAt(Long e) {
            this.expiresAt = e;
            return this;
        }

        public Builder memo(String m) {
            this.memo = m;
            return this;
        }

        public Builder faceDecimals(int d) {
            this.faceDecimals = d;
            return this;
        }

        public Builder backingStrategy(String strategy) {
            this.backingStrategy = strategy;
            return this;
        }

        public Builder issuanceRatio(Double ratio) {
            this.issuanceRatio = ratio;
            return this;
        }

        public Builder issuerSignature(String sig) {
            this.issuerSignature = sig;
            return this;
        }

        public Builder issuerPublicKey(String pubKey) {
            this.issuerPublicKey = pubKey;
            return this;
        }

        public Builder merchantMetadata(String metadata) {
            this.merchantMetadata = metadata;
            return this;
        }

        /**
         * Builds the VoucherSecret instance.
         *
         * @return a new VoucherSecret
         */
        public VoucherSecret build() {
            VoucherSecret vs;
            UUID id = voucherId != null ? voucherId : UUID.randomUUID();

            if (nonce != null) {
                vs = new VoucherSecret(id, nonce);
            } else {
                vs = new VoucherSecret(id);
            }

            if (issuerId != null) vs.setIssuerId(issuerId);
            if (unit != null) vs.setUnit(unit);
            if (faceValue != null) vs.setFaceValue(faceValue);
            vs.setExpiresAt(expiresAt);
            vs.setMemo(memo);
            if (faceDecimals != null) vs.setFaceDecimals(faceDecimals);
            vs.setBackingStrategy(backingStrategy);
            vs.setIssuanceRatio(issuanceRatio);
            if (issuerSignature != null) vs.setIssuerSignature(issuerSignature);
            if (issuerPublicKey != null) vs.setIssuerPublicKey(issuerPublicKey);
            vs.setMerchantMetadata(merchantMetadata);

            return vs;
        }
    }
}
