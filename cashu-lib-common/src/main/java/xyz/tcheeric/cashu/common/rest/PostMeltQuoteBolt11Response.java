package xyz.tcheeric.cashu.common.rest;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PostMeltQuoteBolt11Response extends PostMeltQuoteResponse {

    public PostMeltQuoteBolt11Response(String quoteId, int amount, int feeReserve, boolean paid, int expiry) {
        super(quoteId, amount, feeReserve, paid, expiry);
    }
}
