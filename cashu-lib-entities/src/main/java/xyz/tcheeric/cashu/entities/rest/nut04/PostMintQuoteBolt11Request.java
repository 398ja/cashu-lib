package xyz.tcheeric.cashu.entities.rest.nut04;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PostMintQuoteBolt11Request extends PostMintQuoteRequest {

    public PostMintQuoteBolt11Request(int amount) {
        super(amount, "sat");
    }
}
