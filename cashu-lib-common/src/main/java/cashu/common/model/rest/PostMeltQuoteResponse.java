package cashu.common.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class PostMeltQuoteResponse {

    @JsonProperty("quote")
    private final String quoteId;

    @JsonProperty
    private final int amount;

    @JsonProperty("fee_reserve")
    private final int feeReserve;

    @JsonProperty
    private final boolean paid;

    @JsonProperty
    private final int expiry;
}
