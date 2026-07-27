package xyz.tcheeric.cashu.entities.rest.nut04;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostMintQuoteResponse {

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
    }
}
