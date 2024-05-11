package cashu.common.model.rest;

public class PostMintQuoteBolt11Request extends PostMintQuoteRequest {

    public PostMintQuoteBolt11Request(int amount) {
        super(amount, "sat");
    }
}
