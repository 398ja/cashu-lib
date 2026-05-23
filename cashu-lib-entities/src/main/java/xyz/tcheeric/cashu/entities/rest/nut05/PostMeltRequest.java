package xyz.tcheeric.cashu.entities.rest.nut05;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.entities.rest.PostInputRequest;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class PostMeltRequest<T extends Secret> extends PostInputRequest<T> {

    /**
     * Maximum number of NUT-08 change outputs allowed per melt request. Mirrors
     * the {@code MAX_OUTPUTS} cap on {@code PostMintRequest} for DoS resistance.
     */
    public static final int MAX_OUTPUTS = 1000;

    public PostMeltRequest(@NonNull String quoteId, @NonNull List<Proof<T>> proofs) {
        super(proofs);
        this.quoteId = quoteId;
    }

    public PostMeltRequest(@NonNull String quoteId,
                           @NonNull List<Proof<T>> proofs,
                           List<BlindedMessage> outputs) {
        super(proofs);
        this.quoteId = quoteId;
        this.outputs = outputs;
    }

    @JsonProperty("quote")
    @NotBlank(message = "Quote ID is required")
    private String quoteId;

    /**
     * NUT-08 (Lightning fee return) — optional blinded outputs the wallet
     * provides for the mint to sign with the overpayment difference
     * {@code sum(proofs) - invoice - exactFeeReserve}. The mint MUST ignore
     * this list when the difference is zero or negative.
     *
     * <p>JSON wire name is {@code outputs} per the canonical spec.
     */
    @JsonProperty("outputs")
    @Size(max = MAX_OUTPUTS, message = "Maximum " + MAX_OUTPUTS + " outputs allowed")
    @Valid
    private List<BlindedMessage> outputs;

}
