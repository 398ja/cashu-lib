package xyz.tcheeric.cashu.common.model.rest;

import lombok.NonNull;

public class PostMeltQuoteMockResponse extends PostMeltQuoteResponse {

    public PostMeltQuoteMockResponse(@NonNull String quoteId, int amount, int feeReserve, boolean paid, int expiry) {
        super(quoteId, amount, feeReserve, paid, expiry);
    }
}
