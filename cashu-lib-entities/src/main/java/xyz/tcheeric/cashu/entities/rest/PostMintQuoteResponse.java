package xyz.tcheeric.cashu.entities.rest;

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

    @JsonProperty
    @Builder.Default
    private boolean paid = false;

    @JsonProperty
    private int expiry;
}
