package cashu.common.model.rest;

import lombok.Data;
import lombok.NonNull;

@Data
public class PostMintQuoteFiatRequest extends PostMintQuoteRequest {

    private final String unit;

    public PostMintQuoteFiatRequest(int amount, @NonNull String unit) {
        super(amount);
        this.unit = unit;
    }
}
