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
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.nut12.DLEQProof;

/**
 * Proof structure for NUT-18 payment payloads.
 *
 * <p>Includes DLEQ proof for offline verification by the receiver.
 * This allows receivers to verify proof validity without contacting the mint.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/18.md">NUT-18 Specification</a>
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/12.md">NUT-12 DLEQ Proofs</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"amount", "id", "secret", "C", "dleq", "witness"})
public class PaymentPayloadProof {

    @JsonProperty("amount")
    private int amount;

    @JsonProperty("id")
    private String keysetId;

    @JsonProperty("secret")
    private String secret;

    @JsonProperty("C")
    private String signature;

    @JsonProperty("dleq")
    private PaymentPayloadDLEQ dleq;

    @JsonProperty("witness")
    private String witness;

    /**
     * Creates a PaymentPayloadProof from a standard Proof.
     *
     * @param proof the source proof
     * @param <T> the secret type
     * @return a PaymentPayloadProof with DLEQ if available
     */
    public static <T extends Secret> PaymentPayloadProof fromProof(@NonNull Proof<T> proof) {
        PaymentPayloadProofBuilder builder = PaymentPayloadProof.builder()
                .amount(proof.getAmount())
                .keysetId(proof.getKeySetId())
                .secret(proof.getSecret().toString())
                .signature(proof.getUnblindedSignature().toString());

        if (proof.hasDLEQProof()) {
            DLEQProof dleq = proof.getDleq();
            builder.dleq(PaymentPayloadDLEQ.builder()
                    .e(dleq.getE())
                    .s(dleq.getS())
                    .r(dleq.getR())
                    .build());
        }

        if (proof.getWitness() != null) {
            builder.witness(proof.getWitness().toString());
        }

        return builder.build();
    }

    /**
     * Checks if this proof includes a DLEQ proof.
     *
     * @return true if DLEQ proof is present
     */
    @JsonIgnore
    public boolean hasDLEQ() {
        return dleq != null;
    }

    /**
     * Checks if this proof includes a DLEQ proof with the blinding factor.
     *
     * @return true if DLEQ proof with blinding factor is present
     */
    @JsonIgnore
    public boolean hasDLEQWithBlindingFactor() {
        return dleq != null && dleq.hasBlindingFactor();
    }

    /**
     * DLEQ proof structure for payment payloads.
     *
     * <p>Contains the challenge (e), response (s), and optional blinding factor (r)
     * for offline signature verification.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"e", "s", "r"})
    public static class PaymentPayloadDLEQ {

        @JsonProperty("e")
        private String e;

        @JsonProperty("s")
        private String s;

        @JsonProperty("r")
        private String r;

        /**
         * Checks if this DLEQ proof includes the blinding factor.
         *
         * @return true if the blinding factor (r) is present
         */
        @JsonIgnore
        public boolean hasBlindingFactor() {
            return r != null && !r.isEmpty();
        }

        /**
         * Creates a PaymentPayloadDLEQ from a standard DLEQProof.
         *
         * @param dleq the source DLEQ proof
         * @return a PaymentPayloadDLEQ
         */
        public static PaymentPayloadDLEQ fromDLEQProof(@NonNull DLEQProof dleq) {
            return PaymentPayloadDLEQ.builder()
                    .e(dleq.getE())
                    .s(dleq.getS())
                    .r(dleq.getR())
                    .build();
        }
    }
}
