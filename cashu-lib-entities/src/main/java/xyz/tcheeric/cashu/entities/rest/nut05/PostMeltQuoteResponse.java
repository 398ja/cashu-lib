package xyz.tcheeric.cashu.entities.rest.nut05;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A NUT-05 / NUT-23 melt quote response.
 *
 * <p>{@link #state} supersedes the deprecated {@link #paid} boolean and is the only field that can
 * report an in-flight payment as {@link MeltQuoteState#PENDING}. The two are kept consistent in
 * both directions so a v0 consumer reading {@code paid} and a modern wallet reading {@code state}
 * never disagree.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/05.md">NUT-05</a>
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/23.md">NUT-23</a>
 */
@NoArgsConstructor
@Builder
@Data
@AllArgsConstructor
@JsonPropertyOrder({"quote", "request", "amount", "unit", "method", "fee_reserve", "state", "expiry",
        "payment_preimage", "paid"})
public class PostMeltQuoteResponse {

    /** NUT-23 default payment method, the only one this response type has ever described. */
    public static final String BOLT11_METHOD = "bolt11";

    @JsonProperty("quote")
    private String quoteId;

    /** NUT-23 — the payment request the quote is for. */
    @JsonProperty
    private String request;

    @JsonProperty
    private int amount;

    /** NUT-23 — the unit (for example {@code "sat"}) the quote transacts in. */
    @JsonProperty
    private String unit;

    /** NUT-23 — the payment method, {@code "bolt11"} for a Lightning melt. */
    @JsonProperty
    @Builder.Default
    private String method = BOLT11_METHOD;

    @JsonProperty("fee_reserve")
    private int feeReserve;

    /** NUT-05 — quote lifecycle state: {@code UNPAID}, {@code PENDING} or {@code PAID}. */
    @JsonProperty
    @Builder.Default
    private MeltQuoteState state = MeltQuoteState.UNPAID;

    @JsonProperty
    private int expiry;

    /** NUT-05 — the Lightning payment preimage, present only once the payment has settled. */
    @JsonProperty("payment_preimage")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String paymentPreimage;

    /**
     * @deprecated NUT-05 v0 payment flag, kept on the wire for older clients. Use {@link #state},
     * which alone can express {@link MeltQuoteState#PENDING}.
     */
    @Deprecated
    @JsonProperty
    private boolean paid;

    /**
     * Binary-compatibility constructor matching the pre-NUT-23 all-args descriptor
     * {@code (String, int, int, boolean, int)}, which the bolt11, mock and test subclasses call
     * positionally.
     *
     * @deprecated construct via {@link #builder()} so the NUT-23 fields are set.
     */
    @Deprecated
    public PostMeltQuoteResponse(String quoteId, int amount, int feeReserve, boolean paid, int expiry) {
        this.quoteId = quoteId;
        this.amount = amount;
        this.feeReserve = feeReserve;
        this.expiry = expiry;
        this.method = BOLT11_METHOD;
        setPaid(paid);
    }

    /**
     * Sets the quote state, keeping the deprecated {@code paid} boolean in step.
     */
    public void setState(MeltQuoteState state) {
        this.state = state;
        this.paid = state != null && state.isPaid();
    }

    /**
     * Sets the deprecated {@code paid} boolean, deriving a state from it.
     *
     * <p>A boolean cannot express {@link MeltQuoteState#PENDING}, so a {@code false} received while
     * the quote is already pending leaves the pending state intact rather than downgrading it.
     */
    @Deprecated
    public void setPaid(boolean paid) {
        this.paid = paid;
        if (paid) {
            this.state = MeltQuoteState.PAID;
        } else if (this.state != MeltQuoteState.PENDING) {
            this.state = MeltQuoteState.UNPAID;
        }
    }
}
