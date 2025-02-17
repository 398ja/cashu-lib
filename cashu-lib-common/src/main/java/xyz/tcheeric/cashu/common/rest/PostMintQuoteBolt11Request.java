package xyz.tcheeric.cashu.common.rest;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PostMintQuoteBolt11Request extends PostMintQuoteRequest {

    public PostMintQuoteBolt11Request(int amount) {
        super(amount, "sat");
    }
}
