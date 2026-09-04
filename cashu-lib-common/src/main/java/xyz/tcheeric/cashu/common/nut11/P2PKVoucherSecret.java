package xyz.tcheeric.cashu.common.nut11;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.nut18.VoucherTags;

import java.util.List;

/**
 * A voucher that is also P2PK-locked.
 *
 * <p>Carries issuer-signed voucher metadata in its NUT-10 tags and a spending key in
 * {@code data} that the mint requires a witness signature from. It is one secret with two
 * jobs, which is what its two parents' concerns are: the tags say what the credential is
 * <em>worth</em>, and the lock says who may <em>spend</em> it.
 *
 * <h2>Why a distinct kind</h2>
 *
 * <p>A Cashu proof carries exactly one NUT-10 kind, so a voucher that is also locked has to
 * pick one. Neither existing kind works:
 *
 * <ul>
 *   <li>A {@link xyz.tcheeric.cashu.common.nut18.VoucherSecret} carrying P2PK tags is
 *       dispatched by a mint to its voucher condition, which checks the issuer signature and
 *       expiry but never the witness. The lock would be advisory: a thief holding the proof
 *       could still spend it.</li>
 *   <li>A plain {@link P2PKSecret} carrying voucher tags is enforced, but the issuer
 *       signature is computed over canonical bytes that commit to the kind and to the voucher
 *       id in {@code data}. Moving to {@code P2PK} would leave the signature covering a
 *       document that never exists on the wire, and it would demote the voucher id to a tag
 *       while {@code data} became the spending key.</li>
 * </ul>
 *
 * <h2>Inheritance</h2>
 *
 * <p>Extends {@link P2PKSecret} rather than duplicating it, so NUT-11's malformed-secret rules
 * — repeated tags, thresholds out of range, unrecognised sigflags, duplicate keys within a
 * pathway, non-point public keys — apply here unchanged, and a mint that already handles a
 * {@code P2PKSecret} handles this one. The voucher accessors are added on top.
 *
 * <p>Consequence for callers: {@code instanceof P2PKSecret} is true for this type. That is
 * usually what you want, since the lock really must be enforced. Where a mint needs to
 * distinguish the two — to run the voucher checks as well — it must test for this class
 * <em>first</em>, because a broader branch that matches {@code P2PKSecret} or "is a voucher"
 * will otherwise swallow it and skip half the conditions.
 *
 * @see P2PKSecret
 * @see xyz.tcheeric.cashu.common.nut18.VoucherSecret
 */
@Slf4j
public class P2PKVoucherSecret extends P2PKSecret {

    /**
     * Creates an empty secret. Deserialization uses this; a caller building one should set the
     * spending key, the issuer, and the issuer signature before it is spendable.
     */
    public P2PKVoucherSecret() {
        super();
        setKind(Kind.P2PK_VOUCHER);
    }

    /**
     * Creates a secret locked to {@code spendingKey}.
     *
     * @param spendingKey compressed secp256k1 public key the witness must sign for; validated
     *                    here so a construction-side mistake surfaces where it is made
     */
    public P2PKVoucherSecret(@NonNull byte[] spendingKey) {
        super(spendingKey);
        setKind(Kind.P2PK_VOUCHER);
    }

    /**
     * Creates a secret locked to {@code spendingKey} with an explicit signature threshold and
     * flag.
     *
     * @param spendingKey compressed secp256k1 public key the witness must sign for
     * @param nSigs       required signature count
     * @param sigFlag     whether signatures cover the inputs or the whole transaction
     */
    public P2PKVoucherSecret(@NonNull byte[] spendingKey, int nSigs, @NonNull SignatureFlag sigFlag) {
        super(spendingKey, nSigs, sigFlag);
        setKind(Kind.P2PK_VOUCHER);
    }

    // ===== Voucher metadata =====
    //
    // Deliberately the same tag keys as VoucherSecret (VoucherTags), so an issuer signing a
    // voucher and a mint reading one work with a single vocabulary regardless of which kind
    // carries it. Only the key location differs: the voucher id is a tag here, because `data`
    // holds the spending key.

    /**
     * The voucher id, or {@code null} when unset.
     *
     * <p>A tag rather than {@code data}, which an ordinary {@code VoucherSecret} uses, because
     * {@code data} is where NUT-11 puts the spending key and the lock has to be where the mint
     * looks for it. The key comes from {@link VoucherTags} like every other, so the issuer that
     * signs over it and the mint that reads it back cannot drift apart silently.
     */
    public String getVoucherId() {
        return tagValue(VoucherTags.VOUCHER_ID);
    }

    public void setVoucherId(@NonNull String voucherId) {
        setTag(VoucherTags.VOUCHER_ID, List.of(voucherId));
    }

    /** The issuing merchant's identifier, or {@code null} when unset. */
    public String getIssuerId() {
        return tagValue(VoucherTags.ISSUER);
    }

    public void setIssuerId(@NonNull String issuerId) {
        setTag(VoucherTags.ISSUER, List.of(issuerId));
    }

    /** The currency unit, or {@code null} when unset. */
    public String getUnit() {
        return tagValue(VoucherTags.UNIT);
    }

    public void setUnit(@NonNull String unit) {
        setTag(VoucherTags.UNIT, List.of(unit));
    }

    /** The face value in minor units, or {@code null} when unset. */
    public Long getFaceValue() {
        return longValue(VoucherTags.FACE_VALUE);
    }

    public void setFaceValue(long faceValue) {
        setTag(VoucherTags.FACE_VALUE, List.of(faceValue));
    }

    /** Expiry as a Unix timestamp in seconds, or {@code null} when the voucher does not expire. */
    public Long getExpiresAt() {
        return longValue(VoucherTags.EXPIRES_AT);
    }

    public void setExpiresAt(Long expiresAt) {
        if (expiresAt != null) {
            setTag(VoucherTags.EXPIRES_AT, List.of(expiresAt));
        }
    }

    /** The issuer's signature over this secret's canonical bytes, or {@code null} when unsigned. */
    public String getIssuerSignature() {
        return tagValue(VoucherTags.ISSUER_SIG);
    }

    public void setIssuerSignature(@NonNull String signature) {
        setTag(VoucherTags.ISSUER_SIG, List.of(signature));
    }

    /** The issuer's public key, or {@code null} when unset. */
    public String getIssuerPublicKey() {
        return tagValue(VoucherTags.ISSUER_PUBKEY);
    }

    public void setIssuerPublicKey(@NonNull String publicKey) {
        setTag(VoucherTags.ISSUER_PUBKEY, List.of(publicKey));
    }

    /** Opaque merchant data, or {@code null} when unset. */
    public String getMerchantMetadata() {
        return tagValue(VoucherTags.MERCHANT_METADATA);
    }

    public void setMerchantMetadata(String metadata) {
        if (metadata != null) {
            setTag(VoucherTags.MERCHANT_METADATA, List.of(metadata));
        }
    }

    /** Free-text note from the issuer, or {@code null} when unset. */
    public String getMemo() {
        return tagValue(VoucherTags.MEMO);
    }

    public void setMemo(String memo) {
        if (memo != null) {
            setTag(VoucherTags.MEMO, List.of(memo));
        }
    }

    /**
     * Decimal places of the face value, defaulting to 0.
     *
     * <p>Same defaults as {@link xyz.tcheeric.cashu.common.nut18.VoucherSecret}, deliberately:
     * the two kinds carry the same tags and a reader that gets a different answer depending on
     * which one it holds would make the lock change the money.
     */
    public int getFaceDecimals() {
        Long value = longValue(VoucherTags.FACE_DECIMALS);
        return value != null ? value.intValue() : 0;
    }

    public void setFaceDecimals(int faceDecimals) {
        setTag(VoucherTags.FACE_DECIMALS, List.of(faceDecimals));
    }

    /** How the voucher is backed, defaulting to {@code FIXED} as the plain kind does. */
    public String getBackingStrategy() {
        String value = tagValue(VoucherTags.BACKING_STRATEGY);
        return value != null ? value : "FIXED";
    }

    public void setBackingStrategy(String backingStrategy) {
        if (backingStrategy != null) {
            setTag(VoucherTags.BACKING_STRATEGY, List.of(backingStrategy));
        }
    }

    /** Face value per unit of backing, defaulting to 1.0 as the plain kind does. */
    public double getIssuanceRatio() {
        String value = tagValue(VoucherTags.ISSUANCE_RATIO);
        if (value == null) {
            return 1.0d;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException malformed) {
            log.warn("p2pk_voucher_secret get_issuance_ratio invalid_format value={}", value);
            return 1.0d;
        }
    }

    public void setIssuanceRatio(double issuanceRatio) {
        setTag(VoucherTags.ISSUANCE_RATIO, List.of(issuanceRatio));
    }

    /**
     * NUT-11's rules, plus one this kind adds: a {@code locktime} requires {@code refund} keys.
     *
     * <p>Under NUT-11 a proof whose locktime has passed with no refund keys is spendable with no
     * witness at all. For a plain {@code P2PKSecret} that is correct and deliberate — an escrow
     * that falls open is recoverable rather than burned — and it is left untouched.
     *
     * <p>For a voucher it silently retracts the only guarantee this kind exists to provide. The
     * lock is what makes a stolen proof worthless; a past locktime with no refund path makes
     * possession alone sufficient, which is precisely the property that ruled out reusing the
     * plain {@code VOUCHER} kind. Worse, it degrades quietly and on a timer: the voucher works
     * exactly as intended until the locktime passes, and then stops being locked with nothing
     * observable changing.
     *
     * <p>So the combination is refused where it is created rather than where it is spent.
     * Verification keeps NUT-11 semantics unchanged, because a mint must remain able to spend
     * proofs issued by others; making the secret unconstructable removes the footgun without
     * deviating from the spec for anybody else's proofs.
     *
     * <p>A {@code locktime} with refund keys is still allowed: that is a real reclaim path with
     * a real signature requirement, not an absence of one.
     *
     * @throws MalformedP2PKSecretException if the Proof must be rejected as unspendable
     */
    @Override
    public void validate() {
        super.validate();

        if (getTag(P2PKTag.locktime.name()) != null && getRefund().isEmpty()) {
            throw new MalformedP2PKSecretException(
                    "a P2PK_VOUCHER with a locktime must also carry refund keys: once the locktime "
                            + "passes, NUT-11 makes a refund-less proof spendable with no witness, "
                            + "which would silently unlock the voucher");
        }
    }

    // ===== Helpers =====

    /**
     * Whether the voucher's expiry has passed.
     *
     * <p>Says nothing about the lock or the issuer signature, and nothing about whether the
     * proof is still unspent — that is mint-local state and only a NUT-07 state check answers
     * it.
     */
    public boolean isExpired() {
        Long expiresAt = getExpiresAt();
        return expiresAt != null && System.currentTimeMillis() / 1000 > expiresAt;
    }

    /**
     * Whether both issuer fields are present.
     *
     * <p>Presence only. Verifying the signature needs the canonical bytes and the issuer's key,
     * which live in the voucher domain rather than here.
     */
    public boolean isSigned() {
        return getIssuerSignature() != null && getIssuerPublicKey() != null;
    }

    /**
     * First value of {@code key} as a string, or {@code null}.
     *
     * <p>NUT-10 tags hold strings, but a hand-built secret can hold a boxed number, so the
     * value is coerced rather than cast — a getter must not throw where {@code validate()} is
     * the place that rejects.
     */
    private String tagValue(String key) {
        WellKnownSecret.Tag tag = getTag(key);
        if (tag == null || tag.getValues() == null || tag.getValues().isEmpty()) {
            return null;
        }
        Object value = tag.getValues().get(0);
        return value == null ? null : String.valueOf(value);
    }

    /** First value of {@code key} as a long, or {@code null} when absent or not a number. */
    private Long longValue(String key) {
        String raw = tagValue(key);
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("p2pk_voucher_secret non_numeric_tag key={}", key);
            return null;
        }
    }
}
