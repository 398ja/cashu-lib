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
    private long expiry;
}
