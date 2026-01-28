package xyz.tcheeric.cashu.common.nut17;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for quote state change notifications (mint/melt quotes).
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteStatePayload {
    /**
     * The quote ID.
     */
    @JsonProperty("quote")
    private String quoteId;

    /**
     * The bolt11 payment request (for mint quotes).
     */
    private String request;

    /**
     * Whether the quote has been paid.
     */
    private Boolean paid;

    /**
     * The state of the quote: UNPAID, PENDING, PAID, ISSUED.
     */
    private String state;

    /**
     * Expiration timestamp (Unix seconds).
     */
    private Long expiry;

    /**
     * Amount in satoshis.
     */
    private Long amount;

    /**
     * Fee reserve in satoshis (for melt quotes).
     */
    @JsonProperty("fee_reserve")
    private Long feeReserve;

    /**
     * Payment preimage (for paid melt quotes).
     */
    @JsonProperty("payment_preimage")
    private String paymentPreimage;

    /**
     * Change outputs (for melt quotes with overpayment).
     */
    private Object change;
}
