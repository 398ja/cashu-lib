package xyz.tcheeric.cashu.entities.rest;

import lombok.NonNull;

public class PostMeltQuoteTestResponse extends PostMeltQuoteResponse {

    public PostMeltQuoteTestResponse(@NonNull String quoteId, int amount, int feeReserve, boolean paid, int expiry) {
        super(quoteId, amount, feeReserve, paid, expiry);
    }
}
