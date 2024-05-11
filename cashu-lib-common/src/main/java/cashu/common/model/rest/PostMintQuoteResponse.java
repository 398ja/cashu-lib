package cashu.common.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public class PostMintQuoteResponse {

    @JsonProperty("quote")
    private final String quoteId;

    @JsonProperty
    private final String request;

    @JsonProperty
    @Builder.Default
    private boolean paid = false;

    @JsonProperty
    private int expiry;
}
