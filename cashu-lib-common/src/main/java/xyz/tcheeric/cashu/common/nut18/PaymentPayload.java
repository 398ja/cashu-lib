package xyz.tcheeric.cashu.common.nut18;

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
 * Payment payload for NUT-18 payment request fulfillment.
 *
 * <p>Sent by the sender to the receiver via the transport method specified
 * in the payment request. Contains proofs with DLEQ for offline verification.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/18.md">NUT-18 Specification</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "memo", "mint", "unit", "proofs"})
public class PaymentPayload {

    @JsonProperty("id")
    private String id;

    @JsonProperty("memo")
    private String memo;

    @JsonProperty("mint")
    private String mint;

    @JsonProperty("unit")
    private String unit;

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
     * Creates a PaymentPayload from a PaymentRequest.
     *
     * @param request the payment request being fulfilled
     * @param proofs the proofs to include
     * @param mint the mint URL
     * @param unit the unit
     * @return a new PaymentPayload
     */
    public static PaymentPayload fromRequest(
            @NonNull PaymentRequest request,
            @NonNull List<PaymentPayloadProof> proofs,
            @NonNull String mint,
            @NonNull String unit) {
        return PaymentPayload.builder()
                .id(request.getPaymentId())
                .mint(mint.replaceAll("/+$", ""))
                .unit(unit)
                .proofs(proofs)
                .build();
    }

    /**
     * Creates a PaymentPayload from a PaymentRequest with a memo.
     *
     * @param request the payment request being fulfilled
     * @param proofs the proofs to include
     * @param mint the mint URL
     * @param unit the unit
     * @param memo optional sender memo
     * @return a new PaymentPayload
     */
    public static PaymentPayload fromRequest(
            @NonNull PaymentRequest request,
            @NonNull List<PaymentPayloadProof> proofs,
            @NonNull String mint,
            @NonNull String unit,
            String memo) {
        return PaymentPayload.builder()
                .id(request.getPaymentId())
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
            throw new RuntimeException("Failed to serialize PaymentPayload", e);
        }
    }

    /**
     * Deserializes a PaymentPayload from JSON.
     *
     * @param json the JSON string
     * @return the deserialized PaymentPayload
     * @throws RuntimeException if deserialization fails
     */
    public static PaymentPayload fromJson(@NonNull String json) {
        try {
            return JsonUtils.JSON_MAPPER.readValue(json, PaymentPayload.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize PaymentPayload", e);
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
}
