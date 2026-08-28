package xyz.tcheeric.cashu.common.nut00;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the codes in {@link CashuErrorCode} against the NUT registry in {@code error_codes.md}.
 */
class CashuErrorCodeTest {

    /** Spot-checks that registered codes carry the exact numbers from the NUT registry. */
    @ParameterizedTest
    @CsvSource({
            "proof_verification_failed,10001",
            "proofs_already_spent,11001",
            "proofs_pending,11002",
            "outputs_already_signed,11003",
            "outputs_pending,11004",
            "transaction_not_balanced,11005",
            "amount_outside_limit_range,11006",
            "duplicate_inputs,11007",
            "duplicate_outputs,11008",
            "multiple_units,11009",
            "inputs_outputs_unit_mismatch,11010",
            "amountless_invoice_not_supported,11011",
            "amount_not_equal_invoice,11012",
            "unit_not_supported,11013",
            "max_inputs_exceeded,11014",
            "max_outputs_exceeded,11015",
            "duplicate_quote_ids,11016",
            "max_batch_size_exceeded,11017",
            "keyset_not_known,12001",
            "keyset_inactive,12002",
            "keyset_expired,12003",
            "quote_not_paid,20001",
            "quote_already_issued,20002",
            "minting_disabled,20003",
            "lightning_payment_failed,20004",
            "quote_pending,20005",
            "invoice_already_paid,20006",
            "quote_expired,20007",
            "mint_signature_invalid,20008",
            "pubkey_required_for_mint_quote,20009",
            "clear_auth_required,30001",
            "clear_auth_failed,30002",
            "blind_auth_required,31001",
            "blind_auth_failed,31002",
            "max_bat_mint_amount_exceeded,31003",
            "bat_mint_rate_limit_exceeded,31004"
    })
    void shouldCarrySpecNumberWhenCodeIsRegistered(String key, int expectedCode) {
        // Arrange & Act
        CashuErrorCode errorCode = CashuErrorCode.valueOf(key);

        // Assert
        assertThat(errorCode.getCode()).isEqualTo(expectedCode);
        assertThat(errorCode.isExtension()).isFalse();
    }

    /** Our own errors must not squat on any number the NUT registry could assign. */
    @ParameterizedTest
    @EnumSource(CashuErrorCode.class)
    void shouldKeepExtensionCodesInTheReservedRange(CashuErrorCode errorCode) {
        // Arrange
        boolean inReservedRange = errorCode.getCode() >= CashuErrorCode.EXTENSION_RANGE_START
                && errorCode.getCode() <= CashuErrorCode.EXTENSION_RANGE_END;

        // Act & Assert
        assertThat(errorCode.isExtension()).isEqualTo(inReservedRange);
        if (!errorCode.isExtension()) {
            assertThat(errorCode.getCode()).isLessThan(CashuErrorCode.EXTENSION_RANGE_START);
        }
    }

    /** A numeric code resolves back to a constant carrying that same number. */
    @ParameterizedTest
    @EnumSource(CashuErrorCode.class)
    void shouldResolveBackToACodeCarryingTheSameNumber(CashuErrorCode errorCode) {
        // Act
        CashuErrorCode resolved = CashuErrorCode.forCode(errorCode.getCode()).orElseThrow();

        // Assert
        assertThat(resolved.getCode()).isEqualTo(errorCode.getCode());
    }

    /** An unregistered number resolves to nothing rather than to a misleading constant. */
    @Test
    void shouldResolveToNothingWhenCodeIsUnknown() {
        // Act & Assert
        assertThat(CashuErrorCode.forCode(1337)).isEmpty();
    }

    /** The string key downstream switch sites use is the constant name, unchanged. */
    @Test
    void shouldExposeLegacyStringKeyAsConstantName() {
        // Act & Assert
        assertThat(CashuErrorCode.keyset_inactive.getKey()).isEqualTo("keyset_inactive");
        assertThat(CashuErrorCode.forKey("keyset_inactive")).contains(CashuErrorCode.keyset_inactive);
        assertThat(CashuErrorCode.forKey("no_such_key")).isEmpty();
    }

    /** Every code must state an HTTP status, so a REST layer never has to guess one. */
    @ParameterizedTest
    @EnumSource(CashuErrorCode.class)
    void shouldCarryAPlausibleHttpStatus(CashuErrorCode errorCode) {
        // Act & Assert
        assertThat(errorCode.getHttpStatus()).isBetween(400, 599);
        assertThat(errorCode.getDefaultDetail()).isNotBlank();
    }

    /**
     * Every string key the mint and gateway raise today must still exist as a constant, otherwise
     * their switch statements stop compiling.
     */
    @Test
    void shouldPreserveEveryLegacyStringKey() {
        // Arrange
        List<String> legacyKeys = List.of(
                "amount_mismatch", "face_value_not_backed", "funding_required", "internal_error",
                "invalid_denominations", "invalid_iou_amount", "invalid_output_amount",
                "invalid_quote_amount", "invalid_signature_flag", "iou_not_permitted",
                "iou_not_swappable", "issuance_in_progress", "insufficient_input",
                "keyset_inactive", "keyset_not_found", "melt_in_progress",
                "melt_invoice_not_paid_error", "melt_proof_amount_error", "melt_proof_pending_error",
                "melt_proof_refund_failed", "melt_proof_verification_error", "melt_verify_crypto_error",
                "mint_amount_mismatch", "mint_invoice_not_paid_error",
                "mint_request_contains_null_output", "mint_request_missing_outputs", "mint_suspended",
                "missing_keyset_id", "mixed_proof_types_error", "output_witness_signature",
                "proofs_not_bound", "quote_already_issued", "quote_amount_cross_check_failed",
                "quote_expired", "quote_not_found", "sign_private_key_not_found",
                "swap_mint_not_found", "too_many_inputs", "too_many_outputs",
                "validate_amounts_error", "validate_fees_error",
                "verify_invalid_number_of_signatures", "verify_invalid_refund_signature",
                "verify_locktime_not_reached", "verify_proof_already_used_error",
                "verify_proof_failed_error", "verify_proof_key_set_id_error",
                "verify_proof_key_set_not_found", "voucher_expired", "voucher_master_secret_missing",
                "voucher_quote_not_found", "voucher_signature_invalid",
                "voucher_split_amount_mismatch");

        // Act
        List<String> known = Arrays.stream(CashuErrorCode.values()).map(Enum::name).toList();

        // Assert
        assertThat(known).containsAll(legacyKeys);
    }
}
