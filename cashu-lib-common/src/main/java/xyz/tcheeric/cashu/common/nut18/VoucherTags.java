package xyz.tcheeric.cashu.common.nut18;

/**
 * Standard tag keys for NUT-10 VOUCHER secrets.
 * <p>
 * These tags are used to store voucher metadata in the tag-based format
 * for NUT-10 compliance and cross-implementation interoperability.
 * </p>
 */
public interface VoucherTags {

    /**
     * Voucher identifier, for kinds that cannot keep it in {@code data}.
     *
     * <p>A {@code VOUCHER} secret carries its id in {@code data}. A {@code P2PK_VOUCHER}
     * cannot: {@code data} holds the spending key, which is where NUT-11 puts it and where a
     * mint looks for the lock. It lives here rather than beside the kind that needs it because
     * the issuer signs over the tags and a mint reads them back, so a rename must break both
     * sides at compile time rather than silently in production.
     */
    String VOUCHER_ID = "voucher_id";

    /** Merchant/issuer identifier */
    String ISSUER = "issuer";

    /** Currency unit (sat, usd, eur, etc.) */
    String UNIT = "unit";

    /** Face value in smallest unit */
    String FACE_VALUE = "face_value";

    /** Expiry timestamp (Unix epoch seconds) */
    String EXPIRES_AT = "expires_at";

    /** Human-readable description */
    String MEMO = "memo";

    /** Decimal places for face value display (default: 0) */
    String FACE_DECIMALS = "face_decimals";

    /** Backing strategy: FIXED, MINIMAL, PROPORTIONAL (optional, default: FIXED) */
    String BACKING_STRATEGY = "backing_strategy";

    /** Issuance ratio for backing calculation (optional, default: 1.0) */
    String ISSUANCE_RATIO = "issuance_ratio";

    /** Schnorr signature from issuer (hex) */
    String ISSUER_SIG = "issuer_sig";

    /** Issuer's public key (hex) */
    String ISSUER_PUBKEY = "issuer_pubkey";

    /** Merchant-specific metadata (JSON string) */
    String MERCHANT_METADATA = "merchant_metadata";

    /**
     * Issuance warrant: what, outside the issuing service, authorised this issuance.
     *
     * <p>A voucher carries one signature and it is the issuing <em>service's</em>. The stall
     * named in {@link #ISSUER} signs nothing, so {@code issuer} is an assertion by the service
     * rather than a claim proven by the stall. Whoever holds the service key can mint any face
     * value in any stall's name, and the result is byte-identical to a genuine coupon.
     *
     * <p>This tag carries evidence that someone <em>other than the issuing service</em>
     * authorised the issuance. It is inside the signed canonical bytes on purpose: the issuer's
     * signature over the warrant is what stops a compromised service stripping it.
     *
     * <p><b>A warrant attests a SALE, not a voucher.</b> One sale can mint several coupons —
     * a requested quantity, each auto-splitting again when a coupon would be too large to
     * receive — and none of those ids exist when the merchant signs. So the digest covers the
     * sale total and every coupon minted from that sale carries the same warrant. A verifier
     * treats it as a <em>ceiling</em>: this coupon's face value must not exceed the warranted
     * total. See {@code IssuanceWarrant}.
     *
     * <p>Absent means the voucher predates warrants and the issuer signed nothing about it.
     * A warrant with form {@code none} is a different claim: the issuer has signed that
     * nothing authorised this beyond itself. Conflating the two is how a downgrade gets in.
     */
    String ISSUANCE_WARRANT = "issuance_warrant";
}
