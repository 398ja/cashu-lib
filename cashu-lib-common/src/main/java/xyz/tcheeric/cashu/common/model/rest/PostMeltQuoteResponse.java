package xyz.tcheeric.cashu.common.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PostMeltQuoteResponse {

    @JsonProperty("quote")
    private String quoteId;

    @JsonProperty
    private int amount;

    @JsonProperty("fee_reserve")
    private int feeReserve;

    @JsonProperty
    private boolean paid;

    @JsonProperty
    private int expiry;
}
