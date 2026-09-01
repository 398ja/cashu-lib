package xyz.tcheeric.cashu.common.nut00;

import lombok.Getter;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Numeric Cashu error codes as registered in the NUT specification's
 * <a href="https://github.com/cashubtc/nuts/blob/main/error_codes.md">error_codes.md</a>.
 *
 * <p>Constants are named after the string keys this codebase has always used on the wire, so that
 * downstream {@code switch} sites keep compiling once they switch from {@code String} to this enum.
 * That is why the names are lower case rather than the usual Java constant style.
 *
 * <p>Several string keys legitimately share one spec code: the specification registry is coarser
 * than our internal vocabulary (every signature failure is {@code 10001}, every unbalanced
 * transaction is {@code 11005}). {@link #forCode(int)} therefore resolves a number back to the
 * first, canonical constant carrying it.
 *
 * <p>Errors that are extensions of this implementation (vouchers, merchant IOUs, mint lifecycle)
 * have no spec code. They use the reserved {@link #EXTENSION_RANGE_START}..{@link
 * #EXTENSION_RANGE_END} range, which cannot collide with any current or plausibly future spec code.
 */
@Getter
public enum CashuErrorCode {

    // --- NUT spec codes: 10000 range, proof verification -------------------------------------
    proof_verification_failed(10001, 400, "Proof verification failed"),
    verify_proof_failed_error(10001, 400, "Proof verification failed"),
    verify_invalid_number_of_signatures(10001, 400, "Insufficient valid signatures"),
    verify_invalid_refund_signature(10001, 400, "Invalid refund signature"),
    verify_locktime_not_reached(10001, 400, "Locktime not reached"),
    melt_proof_verification_error(10001, 400, "Invalid proof"),
    melt_verify_crypto_error(10001, 400, "Proof signature verification failed"),
    invalid_signature_flag(10001, 400, "Invalid signature flag"),
    output_witness_signature(10001, 400, "Invalid output witness signature"),

    // --- NUT spec codes: 11000 range, transaction integrity ----------------------------------
    proofs_already_spent(11001, 400, "Proofs already spent"),
    verify_proof_already_used_error(11001, 400, "Proof already used"),
    proofs_pending(11002, 400, "Proofs are pending"),
    melt_proof_pending_error(11002, 400, "Proofs are pending"),
    outputs_already_signed(11003, 400, "Outputs already signed"),
    outputs_pending(11004, 400, "Outputs are pending"),
    transaction_not_balanced(11005, 400, "Transaction is not balanced (inputs != outputs)"),
    insufficient_input(11005, 400, "Inputs do not cover the requested outputs"),
    amount_mismatch(11005, 400, "Input amounts do not match outputs"),
    mint_amount_mismatch(11005, 400, "Requested amount does not match the quote"),
    validate_amounts_error(11005, 400, "Input amounts do not match outputs"),
    validate_fees_error(11005, 400, "Fees validation failed"),
    quote_amount_cross_check_failed(11005, 400, "Quote amount cross-check failed"),
    amount_outside_limit_range(11006, 400, "Amount outside of limit range"),
    invalid_quote_amount(11006, 400, "Quote amount outside of the accepted range"),
    duplicate_inputs(11007, 400, "Duplicate inputs provided"),
    duplicate_outputs(11008, 400, "Duplicate outputs provided"),
    multiple_units(11009, 400, "Inputs/Outputs of multiple units"),
    inputs_outputs_unit_mismatch(11010, 400, "Inputs and outputs not of same unit"),
    amountless_invoice_not_supported(11011, 400, "Amountless invoice is not supported"),
    amount_not_equal_invoice(11012, 400, "Amount in request does not equal invoice"),
    unit_not_supported(11013, 400, "Unit in request is not supported"),
    max_inputs_exceeded(11014, 400, "Max inputs exceeded"),
    too_many_inputs(11014, 400, "Max inputs exceeded"),
    max_outputs_exceeded(11015, 400, "Max outputs exceeded"),
    too_many_outputs(11015, 400, "Max outputs exceeded"),
    duplicate_quote_ids(11016, 400, "Duplicate quote IDs provided"),
    max_batch_size_exceeded(11017, 400, "Max batch size exceeded"),

    // --- NUT spec codes: 12000 range, keysets -------------------------------------------------
    keyset_not_known(12001, 404, "Keyset is not known"),
    keyset_not_found(12001, 404, "Keyset not found"),
    verify_proof_key_set_not_found(12001, 404, "Keyset not found"),
    verify_proof_key_set_id_error(12001, 400, "Keyset id missing"),
    keyset_inactive(12002, 400, "Keyset is inactive, cannot sign messages"),
    keyset_expired(12003, 400, "Keyset has expired"),

    // --- NUT spec codes: 20000 range, quotes --------------------------------------------------
    quote_not_paid(20001, 400, "Quote request is not paid"),
    mint_invoice_not_paid_error(20001, 400, "Invoice not paid"),
    melt_invoice_not_paid_error(20001, 400, "Invoice not paid"),
    quote_already_issued(20002, 400, "Quote has already been issued"),
    minting_disabled(20003, 400, "Minting is disabled"),
    lightning_payment_failed(20004, 400, "Lightning payment failed"),
    quote_pending(20005, 400, "Quote is pending"),
    issuance_in_progress(20005, 400, "Issuance already in progress for this quote"),
    melt_in_progress(20005, 400, "Melt already in progress for this quote"),
    invoice_already_paid(20006, 400, "Invoice already paid"),
    quote_expired(20007, 400, "Quote is expired"),
    mint_signature_invalid(20008, 400, "Signature for mint request invalid"),
    pubkey_required_for_mint_quote(20009, 400, "Pubkey required for mint quote"),

    // --- NUT spec codes: 30000/31000 ranges, authentication -----------------------------------
    clear_auth_required(30001, 401, "Endpoint requires clear auth"),
    clear_auth_failed(30002, 401, "Clear authentication failed"),
    blind_auth_required(31001, 401, "Endpoint requires blind auth"),
    blind_auth_failed(31002, 401, "Blind authentication failed"),
    max_bat_mint_amount_exceeded(31003, 400, "Maximum BAT mint amount exceeded"),
    bat_mint_rate_limit_exceeded(31004, 429, "BAT mint rate limit exceeded"),

    // --- Implementation extensions: no spec code, reserved 90000 range ------------------------
    invalid_output_amount(90001, 400, "Output amount must be a positive integer"),
    invalid_denominations(90002, 400, "Output denominations are invalid for this keyset"),
    missing_keyset_id(90003, 400, "Output is missing a keyset id"),
    mint_request_missing_outputs(90004, 400, "Mint request contains no outputs"),
    mint_request_contains_null_output(90005, 400, "Mint request contains a null output"),
    mint_suspended(90006, 503, "Mint is suspended and not currently issuing"),
    quote_not_found(90007, 404, "Quote not found"),
    internal_error(90008, 500, "Internal server error"),
    swap_mint_not_found(90009, 404, "Mint not found"),
    mixed_proof_types_error(90010, 400, "Cannot mix voucher and regular proofs"),
    iou_not_permitted(90011, 400, "Merchant IOU funding is not permitted by mint policy"),
    iou_not_swappable(90012, 400, "Merchant IOU proofs cannot be swapped"),
    invalid_iou_amount(90013, 400, "Merchant IOU amount is invalid"),
    face_value_not_backed(90014, 400, "Voucher face value is not backed by a qualifying funding source"),
    funding_required(90015, 400, "Funding is required before this operation"),
    voucher_expired(90016, 400, "Voucher has expired"),
    voucher_master_secret_missing(90017, 400, "Voucher master secret is missing"),
    voucher_quote_not_found(90018, 404, "Voucher quote not found"),
    voucher_signature_invalid(90019, 400, "Voucher signature is invalid"),
    voucher_split_amount_mismatch(90020, 400, "Voucher split amounts do not match the face value"),
    proofs_not_bound(90021, 500, "Failed to durably bind every input proof to the melt saga"),
    sign_private_key_not_found(90022, 500, "Private key not found"),
    melt_proof_amount_error(90023, 400, "Proof amount error"),
    melt_proof_refund_failed(90024, 500, "Failed to refund melt proofs"),
    voucher_not_accepted(90025, 400, "Voucher must be redeemed with the issuing merchant"),
    iou_not_meltable(90026, 400, "Merchant IOU proofs cannot be melted"),
    unsupported_proof_type(90027, 400, "Proof carries a spending condition this mint cannot evaluate"),
    invalid_blind_signature(90028, 500, "Mint produced a malformed blind signature"),
    payment_unknown(90029, 500, "Payment outcome is unknown and awaiting operator review"),
    sigall_missing_inputs(90030, 400, "SIG_ALL proof verified outside a transaction"),
    dleq_generation_failed(90031, 500, "Mint could not produce a DLEQ proof for the signature");

    /** First code reserved for errors this implementation raises that the NUT registry lacks. */
    public static final int EXTENSION_RANGE_START = 90000;

    /** Last code reserved for implementation extensions. */
    public static final int EXTENSION_RANGE_END = 90999;

    private static final Map<Integer, CashuErrorCode> CANONICAL_BY_CODE = Arrays.stream(values())
            .collect(Collectors.toMap(
                    CashuErrorCode::getCode,
                    Function.identity(),
                    (first, duplicate) -> first));

    /** The numeric code placed on the wire. */
    private final int code;

    /** The HTTP status a mint returns alongside this code. */
    private final int httpStatus;

    /** Default human-readable detail, used when a caller supplies no message of its own. */
    private final String defaultDetail;

    CashuErrorCode(int code, int httpStatus, @NonNull String defaultDetail) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultDetail = defaultDetail;
    }

    /**
     * The string key historically used on the wire and in downstream {@code switch} statements.
     */
    public String getKey() {
        return name();
    }

    /** Whether this code is an extension of ours rather than a registered NUT code. */
    public boolean isExtension() {
        return code >= EXTENSION_RANGE_START && code <= EXTENSION_RANGE_END;
    }

    /**
     * Resolves a numeric code to its canonical constant, or empty for a code we do not know.
     */
    public static Optional<CashuErrorCode> forCode(int code) {
        return Optional.ofNullable(CANONICAL_BY_CODE.get(code));
    }

    /**
     * Resolves a string key to its constant, or empty for a key we do not know.
     */
    public static Optional<CashuErrorCode> forKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(candidate -> candidate.name().equals(key)).findFirst();
    }
}
