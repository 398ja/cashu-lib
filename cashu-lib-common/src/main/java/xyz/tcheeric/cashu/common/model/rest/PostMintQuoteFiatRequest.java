package xyz.tcheeric.cashu.common.model.rest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class PostMintQuoteFiatRequest extends PostMintQuoteRequest {

    private String unit;

    public PostMintQuoteFiatRequest(int amount, @NonNull String unit) {
        super(amount);
        this.unit = unit;
    }
}
