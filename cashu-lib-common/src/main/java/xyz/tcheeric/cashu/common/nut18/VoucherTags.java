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
}
