package xyz.tcheeric.cashu.entities.rest.nut05;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.BlindSignature;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostMeltResponse {

    @JsonProperty
    private boolean paid;

    @JsonProperty("payment_preimage")
    private String paymentPreimage;

    /**
     * NUT-08 (Lightning fee return) — when the wallet provided
     * {@code outputs} on the melt request AND the mint observed
     * {@code sum(proofs) > invoice + exactFeeReserve}, the mint signs
     * the change outputs and returns them here. Omitted when no
     * overpayment / no outputs supplied.
     *
     * <p>JSON wire name is {@code change} per the canonical spec; the
     * field is suppressed in serialised form when {@code null}.
     */
    @JsonProperty("change")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<BlindSignature> change;

    public PostMeltResponse(boolean paid, String paymentPreimage) {
        this.paid = paid;
        this.paymentPreimage = paymentPreimage;
    }
}
