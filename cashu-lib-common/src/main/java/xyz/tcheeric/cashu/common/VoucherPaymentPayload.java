package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Payment payload for NUT-18V voucher payment request fulfillment.
 *
 * <p>Sent by the sender to the receiver via the transport method specified
 * in the voucher payment request. Contains proofs with DLEQ for offline verification
 * in Model B gift card scenarios.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/18.md">NUT-18 Specification</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "r", "memo", "mint", "unit", "proofs"})
public class VoucherPaymentPayload {

    /**
     * Payment request identifier being fulfilled.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Issuer identifier from the voucher payment request.
     */
    @JsonProperty("r")
    private String issuerId;

    /**
     * Optional sender memo.
     */
    @JsonProperty("memo")
    private String memo;

    /**
     * Mint URL where the proofs were issued.
     */
    @JsonProperty("mint")
    private String mint;

    /**
     * Unit of the proofs (e.g., "sat", "usd").
     */
    @JsonProperty("unit")
    private String unit;

    /**
     * List of proofs with optional DLEQ for offline verification.
     */
    @JsonProperty("proofs")
    @Builder.Default
    private List<PaymentPayloadProof> proofs = new ArrayList<>();

    /**
     * Sets the mint URL, stripping any trailing slashes.
     *
     * @param mint the mint URL
     */
    public void setMint(String mint) {
        this.mint = mint != null ? mint.replaceAll("/+$", "") : null;
    }

    /**
     * Creates a VoucherPaymentPayload from a VoucherPaymentRequest.
     *
     * @param request the voucher payment request being fulfilled
     * @param proofs the proofs to include
     * @param mint the mint URL
     * @param unit the unit
     * @return a new VoucherPaymentPayload
     */
    public static VoucherPaymentPayload fromRequest(
            @NonNull VoucherPaymentRequest request,
            @NonNull List<PaymentPayloadProof> proofs,
            @NonNull String mint,
            @NonNull String unit) {
        return VoucherPaymentPayload.builder()
                .id(request.getPaymentId())
                .issuerId(request.getIssuerId())
                .mint(mint.replaceAll("/+$", ""))
                .unit(unit)
                .proofs(proofs)
                .build();
    }

    /**
     * Creates a VoucherPaymentPayload from a VoucherPaymentRequest with a memo.
     *
     * @param request the voucher payment request being fulfilled
     * @param proofs the proofs to include
     * @param mint the mint URL
     * @param unit the unit
     * @param memo optional sender memo
     * @return a new VoucherPaymentPayload
     */
    public static VoucherPaymentPayload fromRequest(
            @NonNull VoucherPaymentRequest request,
            @NonNull List<PaymentPayloadProof> proofs,
            @NonNull String mint,
            @NonNull String unit,
            String memo) {
        return VoucherPaymentPayload.builder()
                .id(request.getPaymentId())
                .issuerId(request.getIssuerId())
                .memo(memo)
                .mint(mint.replaceAll("/+$", ""))
                .unit(unit)
                .proofs(proofs)
                .build();
    }

    /**
     * Serializes this payload to JSON.
     *
     * @return the JSON string
     * @throws RuntimeException if serialization fails
     */
    public String toJson() {
        try {
            return JsonUtils.JSON_MAPPER.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize VoucherPaymentPayload", e);
        }
    }

    /**
     * Deserializes a VoucherPaymentPayload from JSON.
     *
     * @param json the JSON string
     * @return the deserialized VoucherPaymentPayload
     * @throws RuntimeException if deserialization fails
     */
    public static VoucherPaymentPayload fromJson(@NonNull String json) {
        try {
            return JsonUtils.JSON_MAPPER.readValue(json, VoucherPaymentPayload.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize VoucherPaymentPayload", e);
        }
    }

    /**
     * Calculates the total amount of all proofs.
     *
     * @return the sum of all proof amounts
     */
    @JsonIgnore
    public int getTotalAmount() {
        if (proofs == null) {
            return 0;
        }
        return proofs.stream().mapToInt(PaymentPayloadProof::getAmount).sum();
    }

    /**
     * Adds a proof to this payload.
     *
     * @param proof the proof to add
     */
    public void addProof(@NonNull PaymentPayloadProof proof) {
        if (this.proofs == null) {
            this.proofs = new ArrayList<>();
        }
        this.proofs.add(proof);
    }

    /**
     * Gets the number of proofs in this payload.
     *
     * @return the proof count
     */
    @JsonIgnore
    public int getProofCount() {
        return proofs != null ? proofs.size() : 0;
    }

    /**
     * Checks if all proofs have DLEQ proofs attached.
     * Required for offline verification.
     *
     * @return true if all proofs have DLEQ
     */
    @JsonIgnore
    public boolean allProofsHaveDLEQ() {
        if (proofs == null || proofs.isEmpty()) {
            return false;
        }
        return proofs.stream().allMatch(PaymentPayloadProof::hasDLEQ);
    }

    /**
     * Validates this payload for offline verification requirements.
     *
     * @throws IllegalStateException if the payload cannot be verified offline
     */
    public void validateForOfflineVerification() {
        if (issuerId == null || issuerId.isBlank()) {
            throw new IllegalStateException("Issuer ID is required for offline verification");
        }
        if (!allProofsHaveDLEQ()) {
            throw new IllegalStateException("All proofs must have DLEQ for offline verification");
        }
    }
}
