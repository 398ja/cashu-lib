package xyz.tcheeric.cashu.entities.rest.nut04;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonPropertyOrder({"quote", "request", "amount", "unit", "method", "amount_paid", "amount_issued",
        "updated_at", "state", "expiry", "paid"})
public class PostMintQuoteResponse {

    /** NUT-23 default payment method, the only one this response type has ever described. */
    public static final String BOLT11_METHOD = "bolt11";

    @JsonProperty("quote")
    private String quoteId;

    @JsonProperty
    private String request;

    /**
     * NUT-04 v1 — the amount the quote was created for. Required by modern
     * cashu wallets (e.g. cashu-ts {@code >= 4.x}), which normalize this
     * field on every mint-quote response and reject an absent value.
     */
    @JsonProperty
    private int amount;

    /** NUT-04 v1 — the unit (e.g. {@code "sat"}) the quote transacts in. */
    @JsonProperty
    private String unit;

    /**
     * NUT-20 — the key this quote is locked to, echoed back from the request, or null when the
     * quote is unlocked.
     */
    @JsonProperty("pubkey")
    private String pubkey;

    /** NUT-23 — the payment method, {@code "bolt11"} for a Lightning mint quote. */
    @JsonProperty
    @Builder.Default
    private String method = BOLT11_METHOD;

    /**
     * NUT-23 — the amount paid into the quote so far. Wallets should prefer this and
     * {@link #amountIssued} over {@link #state}, because together they also describe a partial
     * payment.
     */
    @JsonProperty("amount_paid")
    private long amountPaid;

    /** NUT-23 — the amount already issued as blind signatures against this quote. */
    @JsonProperty("amount_issued")
    private long amountIssued;

    /** NUT-23 — Unix timestamp of the last change to the quote. */
    @JsonProperty("updated_at")
    private long updatedAt;

    /**
     * NUT-04 v1 — quote lifecycle state: {@code "UNPAID"}, {@code "PAID"},
     * or {@code "ISSUED"}. Supersedes the deprecated {@link #paid} boolean,
     * which is retained for backward compatibility with v0 consumers.
     */
    @JsonProperty
    private String state;

    /**
     * @deprecated NUT-04 v0 payment flag. Use {@link #state} instead;
     * kept on the wire for older clients.
     */
    @Deprecated
    @JsonProperty
    @Builder.Default
    private boolean paid = false;

    @JsonProperty
    private int expiry;

    /**
     * Binary-compatibility constructor matching the pre-0.18 all-args
     * descriptor {@code (String, String, boolean, int)}. Lombok's
     * {@code @AllArgsConstructor} now generates a wider signature because of
     * the new NUT-04 v1 fields; this explicit constructor preserves the old
     * one so consumers compiled against 0.16/0.17 do not hit
     * {@code NoSuchMethodError} after swapping in the new jar. New code should
     * use the builder (which sets {@code amount}/{@code unit}/{@code state}).
     *
     * @deprecated construct via {@link #builder()} so the v1 fields are set.
     */
    @Deprecated
    public PostMintQuoteResponse(String quoteId, String request, boolean paid, int expiry) {
        this.quoteId = quoteId;
        this.request = request;
        this.paid = paid;
        this.expiry = expiry;
        this.method = BOLT11_METHOD;
    }
}
