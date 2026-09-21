package xyz.tcheeric.cashu.entities.rest.nut05;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NonNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PostMeltQuoteTestResponse extends PostMeltQuoteResponse {

    public PostMeltQuoteTestResponse(@NonNull String quoteId, int amount, int feeReserve, boolean paid, int expiry) {
        super(quoteId, amount, feeReserve, paid, expiry);
    }
}
