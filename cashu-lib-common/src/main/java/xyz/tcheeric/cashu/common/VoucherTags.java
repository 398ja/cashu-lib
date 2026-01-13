package xyz.tcheeric.cashu.common;

/**
 * Standard tag keys for NUT-10 VOUCHER secrets.
 * <p>
 * These tags are used to store voucher metadata in the tag-based format
 * for NUT-10 compliance and cross-implementation interoperability.
 * </p>
 */
public interface VoucherTags {

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
