package xyz.tcheeric.cashu.common.model.rest;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PostMintQuoteBolt11Request extends PostMintQuoteRequest {

    public PostMintQuoteBolt11Request(int amount) {
        super(amount, "sat");
    }
}
