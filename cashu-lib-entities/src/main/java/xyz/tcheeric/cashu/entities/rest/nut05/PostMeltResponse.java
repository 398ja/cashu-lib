package xyz.tcheeric.cashu.entities.rest.nut05;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.BlindSignature;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonPropertyOrder({"quote", "request", "amount", "unit", "method", "fee_reserve", "state",
        "expiry", "payment_preimage", "paid", "change"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostMeltResponse {

    /**
     * NUT-05 defines the melt response as the melt <em>quote</em> response
     * plus the method-specific proof of payment. This class carried only
     * {@code paid}, {@code payment_preimage} and {@code change} — the
     * pre-NUT-23 shape — and a current wallet rejects that outright, reporting
     * eight missing fields.
     *
     * <p>The consequence is worse than a refusal: the melt has already
     * happened at the mint, the invoice is paid and the inputs are spent, and
     * the wallet cannot parse the answer. An unparseable success looks exactly
     * like a failure, and a wallet that retries has already lost its proofs.
     */
    @JsonProperty("quote")
    private String quoteId;

    @JsonProperty
    private String request;

    @JsonProperty
    private int amount;

    @JsonProperty
    private String unit;

    @JsonProperty
    private String method;

    @JsonProperty("fee_reserve")
    private int feeReserve;

    /**
     * {@code UNPAID}, {@code PENDING} or {@code PAID}.
     *
     * <p>The spec's successor to {@code paid}: a melt in flight is neither
     * paid nor unpaid, and a boolean cannot express that.
     */
    @JsonProperty
    private MeltQuoteState state;

    @JsonProperty
    private int expiry;

    /**
     * @deprecated superseded by {@link #state}. Kept alongside it rather than
     *     replaced, because the two carry the same fact in different
     *     vocabularies and removing the boolean to satisfy new wallets would
     *     break every existing one. Remove once nothing reads it.
     */
    @Deprecated
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
